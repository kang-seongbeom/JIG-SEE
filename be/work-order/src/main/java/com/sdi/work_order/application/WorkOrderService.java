package com.sdi.work_order.application;

import com.sdi.work_order.client.response.JigItemResponseDto;
import com.sdi.work_order.dto.reponse.*;
import com.sdi.work_order.dto.request.WorkOrderAutoCreateRequestDto;
import com.sdi.work_order.dto.request.WorkOrderCreateRequestDto;
import com.sdi.work_order.entity.WorkOrderNosqlEntity;
import com.sdi.work_order.entity.WorkOrderRDBEntity;
import com.sdi.work_order.repository.WorkOrderCriteriaRepository;
import com.sdi.work_order.repository.WorkOrderNosqlRepository;
import com.sdi.work_order.repository.WorkOrderRDBRepository;
import com.sdi.work_order.util.WorkOrderCheckItem;
import com.sdi.work_order.util.WorkOrderItem;
import com.sdi.work_order.util.WorkOrderStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.*;
import java.util.stream.Collectors;

import static com.sdi.work_order.dto.request.WorkOrderUpdateStatusRequestDto.UpdateStatusItem;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class WorkOrderService {

    private final static String SYSTEM = "SYSTEM";

    private final JigItemService jigItemService;
    private final MemberService memberService;
    private final WorkOrderRDBRepository workOrderRDBRepository;
    private final WorkOrderNosqlRepository workOrderNosqlRepository;
    private final WorkOrderCriteriaRepository workOrderCriteriaRepository;

    public WorkOrderDetailResponseDto detail(String accessToken, Long workOrderId) {
        WorkOrderRDBEntity rdb = getRDBWorkOrderById(workOrderId);
        JigItemResponseDto jigItem = getJigItemIncludeDelete(accessToken, rdb.getJigSerialNo());
        WorkOrderNosqlEntity nosql = getNosqlWorkOrderCheckList(rdb.getCheckListId());

        String creator = getMemberInfo(accessToken, rdb.getCreatorEmployeeNo());
        String terminator = getMemberInfo(accessToken, rdb.getTerminatorEmployeeNo());

        return WorkOrderDetailResponseDto.from(rdb, creator, terminator, jigItem, nosql.getCheckList());
    }

    public WorkOrderResponseDto all(String accessToken, WorkOrderStatus status, int page, int size) {
        Pageable pageable = getPageable(page, size);

        Page<WorkOrderRDBEntity> infos = switch (status) {
            case PUBLISH, PROGRESS, FINISH -> workOrderRDBRepository.findByStatusOrderByCreatedAtDesc(status, pageable);
            case null -> workOrderRDBRepository.findAllByOrderByCreatedAtDesc(pageable);
        };
        return mapToWorkOrderResponseDto(infos, mapToWorkOrderItems(accessToken, infos));
    }

    public WorkOrderGroupingResponseDto grouping(String accessToken) {
        List<WorkOrderRDBEntity> findAll = workOrderRDBRepository.findAll();
        List<WorkOrderItem> items = mapToWorkOrderItems(accessToken, findAll);

        Map<WorkOrderStatus, List<WorkOrderItem>> group =
                items.stream()
                        .collect(Collectors.groupingBy(WorkOrderItem::status));
        return WorkOrderGroupingResponseDto.from(group);
    }

    public WorkOrderResponseDto findByPerson(String accessToken, String employeeNo, String name, int page, int size) {
        Pageable pageable = getPageable(page, size);
        if (employeeNo != null && !employeeNo.isBlank()) {
            Page<WorkOrderRDBEntity> infos = workOrderRDBRepository.findAllByCreatorEmployeeNoOrderByCreatedAtDesc(employeeNo, pageable);
            return mapToWorkOrderResponseDto(infos, mapToWorkOrderItems(accessToken, infos));
        } else if (name != null && !name.isBlank()) {
            List<String> memberEmployeeNos = memberService.getMemberEmployeeByNames(accessToken, name);
            Page<WorkOrderRDBEntity> infos = workOrderRDBRepository.findByCreatorEmployeeNoIn(memberEmployeeNos, pageable);
            return mapToWorkOrderResponseDto(infos, mapToWorkOrderItems(accessToken, infos));
        }

        // 사번, 이름 둘 다 null 일 때 전부 조회
        return all(accessToken, null, page, size);
    }

    @Transactional
    public void create(String accessToken, WorkOrderCreateRequestDto dto) {
        // 사번 조회
        String employeeNo = getMemberEmployeeNo(accessToken);

        // 일련번호로 jig 조회
        JigItemResponseDto jigItem = getJigItem(accessToken, dto.serialNo());

        // 같은 Jig Item에 대해서 이미 생성된 WO가 있다면 생성 불가
        isAlreadyExistNotFinishWorkOrderThenThrow(jigItem);

        // 저장할 데이터 생성
        String checkListId = UUID.randomUUID().toString();
        WorkOrderRDBEntity rdb = WorkOrderRDBEntity.from(employeeNo, jigItem.serialNo(), jigItem.model(), WorkOrderStatus.PROGRESS, checkListId);
        WorkOrderNosqlEntity nosql = WorkOrderNosqlEntity.from(checkListId, false, WorkOrderCheckItem.from(jigItem.checkList()));

        saveWorkOrder(rdb, nosql);
    }

    @Transactional
    public void tmpSave(Long id, List<WorkOrderCheckItem> updateCheckList) {
        WorkOrderRDBEntity rdb = getRDBWorkOrderById(id);
        saveDataAndReturnAllPassOrNot(rdb, updateCheckList);
    }

    @Transactional
    public WorkOrderDoneResponseDto save(String accessToken, Long id, List<WorkOrderCheckItem> checkList) {
        String terminatorEmployeeNo = getMemberEmployeeNo(accessToken);

        WorkOrderRDBEntity rdb = getRDBWorkOrderById(id);
        boolean allPassOrNot = saveDataAndReturnAllPassOrNot(rdb, checkList);
        rdb.updateTerminatorEmployeeNo(terminatorEmployeeNo);
        rdb.updateStatus(WorkOrderStatus.FINISH);

        // 수리 이력 생성 및 폐기 판단 요청
        jigItemService.deleteAndRepair(rdb.getJigSerialNo(), allPassOrNot);

        return WorkOrderDoneResponseDto.from(allPassOrNot);
    }

    @Transactional
    public void updateStatus(List<UpdateStatusItem> list) {
        for (UpdateStatusItem item : list) {
            WorkOrderRDBEntity workOrder = getRDBWorkOrderById(item.id());
            if (workOrder.getStatus() == WorkOrderStatus.FINISH) continue; // 이미 종료된 wo는 수정 불가

            workOrder.updateStatus(item.status());
        }
    }

    @Transactional
    public void autoCreate(String accessToken, WorkOrderAutoCreateRequestDto dto) {
        for (String serialNo : dto.serialNos()) {
            JigItemResponseDto jigItem = getJigItem(accessToken, serialNo);

            try {
                isAlreadyExistNotFinishWorkOrderThenThrow(jigItem);
            } catch (IllegalArgumentException e) {
                continue;
            }

            String checkListId = UUID.randomUUID().toString();
            WorkOrderRDBEntity rdb = WorkOrderRDBEntity.from(SYSTEM, jigItem.serialNo(), jigItem.model(), WorkOrderStatus.PUBLISH, checkListId);
            WorkOrderNosqlEntity nosql = WorkOrderNosqlEntity.from(checkListId, false, WorkOrderCheckItem.from(jigItem.checkList()));

            saveWorkOrder(rdb, nosql);
        }
    }

    public WorkOrderStatusResponseDto getStatus(String employeeNo, Integer year, Integer month) {
        Map<String, LocalDateTime> startAndEndDate = integerToLocalDateTime(year, month);

        LocalDateTime startDate = startAndEndDate.get("startDate");
        LocalDateTime endDate = startAndEndDate.get("endDate");

        List<WorkOrderRDBEntity> monthFinishWorkOrderList =
                workOrderRDBRepository.findAllByCreatorEmployeeNoAndStatusAndCreatedAtBetween(
                        employeeNo,
                        WorkOrderStatus.FINISH,
                        startDate,
                        endDate
                );

        int countRepairFinish = 0;
        int countDelete = 0;

        for (WorkOrderRDBEntity workOrder : monthFinishWorkOrderList) {
            // 수리완료 - FINISH && TRUE
            Optional<WorkOrderNosqlEntity> workOrderNosql = workOrderNosqlRepository.findById(workOrder.getCheckListId());
            if (Boolean.TRUE.equals(workOrderNosql.get().getPassOrNot())) countRepairFinish++;
                // 폐기 - FINISH && FALSE
            else if (Boolean.FALSE.equals(workOrderNosql.get().getPassOrNot())) countDelete++;
        }

        // 수리중 - PROGRESS
        int countRepairing = workOrderRDBRepository.countByCreatorEmployeeNoAndStatus(employeeNo, WorkOrderStatus.PROGRESS);

        return WorkOrderStatusResponseDto.of(countRepairFinish, countDelete, countRepairing);
    }

    public WorkOrderCountResponseDto countRepairRequest(Integer year, Integer month) {
        Map<String, LocalDateTime> startAndEndDate = integerToLocalDateTime(year, month);

        LocalDateTime startDate = startAndEndDate.get("startDate");
        LocalDateTime endDate = startAndEndDate.get("endDate");

        int count = workOrderRDBRepository.countByStatusNotAndCreatedAtBetween(WorkOrderStatus.PUBLISH, startDate, endDate);
        return new WorkOrderCountResponseDto(count);
    }

    private void saveWorkOrder(WorkOrderRDBEntity rdb, WorkOrderNosqlEntity nosql) {
        workOrderRDBRepository.save(rdb);
        workOrderNosqlRepository.save(nosql);
    }

    private void isAlreadyExistNotFinishWorkOrderThenThrow(JigItemResponseDto jigItem) {
        workOrderRDBRepository.findByJigSerialNoAndStatusNot(jigItem.serialNo(), WorkOrderStatus.FINISH)
                .ifPresent(item -> {
                            throw new IllegalArgumentException(
                                    String.format("Jig \'%s\'는 종료되지 않은 Work Order가 있습니다.", jigItem.serialNo()));
                        }
                );
    }

    private Pageable getPageable(int page, int size) {
        return PageRequest.of(Math.max(0, page - 1), Math.max(1, size));
    }

    private WorkOrderResponseDto mapToWorkOrderResponseDto(Page<WorkOrderRDBEntity> infos, List<WorkOrderItem> workOrderItems) {
        return WorkOrderResponseDto.of(infos.getNumber() + 1, Math.max(1, infos.getTotalPages()), workOrderItems);
    }

    private List<WorkOrderItem> mapToWorkOrderItems(String accessToken, Iterable<WorkOrderRDBEntity> infos) {
        List<WorkOrderItem> items = new ArrayList<>();

        for (WorkOrderRDBEntity info : infos) {
            String creator = memberService.getMemberInfo(accessToken, info.getCreatorEmployeeNo());
            String terminator = info.getTerminatorEmployeeNo();
            terminator = getMemberInfo(accessToken, terminator);

            items.add(WorkOrderItem.from(info, creator, terminator));
        }
        return items;
    }

    private boolean saveDataAndReturnAllPassOrNot(WorkOrderRDBEntity rdb, List<WorkOrderCheckItem> updateCheckList) {
        WorkOrderNosqlEntity nosql;
        boolean allPassOrNot = false;

        if (rdb.getCheckListId() != null) {
            nosql = getNosqlWorkOrderCheckList(rdb.getCheckListId());
            allPassOrNot = nosql.updateCheckListAndReturnAllPassOrNot(updateCheckList);
        } else {
            // 만약 rdb에 점검 리스트가 저장되어 있지 않다면 생성해서 넣음
            String uuid = UUID.randomUUID().toString();
            nosql = WorkOrderNosqlEntity.from(uuid, false, updateCheckList);
            rdb.updatedCheckListId(uuid);
        }
        workOrderNosqlRepository.save(nosql);
        rdb.updateDate();

        return allPassOrNot;
    }

    private JigItemResponseDto getJigItem(String accessToken, String serialNo) {
        return jigItemService.findBySerialNo(accessToken, serialNo).getResult();
    }

    private JigItemResponseDto getJigItemIncludeDelete(String accessToken, String serialNo) {
        return jigItemService.findBySerialNoIncludeDelete(accessToken, serialNo).getResult();
    }

    private String getMemberInfo(String accessToken, String employeeNo) {
        if (employeeNo != null) {
            return memberService.getMemberInfo(accessToken, employeeNo);
        }
        return null;
    }

    private String getMemberEmployeeNo(String accessToken) {
        return memberService.getMemberEmployeeNo(accessToken);
    }

    private WorkOrderRDBEntity getRDBWorkOrderById(Long id) {
        return workOrderRDBRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException(String.format("id : %d 로 Work order를 찾을 수 없습니다.", id)));
    }

    private WorkOrderNosqlEntity getNosqlWorkOrderCheckList(String checkListId) {
        return workOrderNosqlRepository.findById(checkListId)
                .orElseThrow(() -> new IllegalArgumentException(String.format("id : %s 로 Work order CheckList를 찾을 수 없습니다.", checkListId)));
    }

    private Map<String, LocalDateTime> integerToLocalDateTime(Integer year, Integer month) {
        if (year == null) {
            year = Calendar.getInstance().get(Calendar.YEAR);
        }
        if (month == null) {
            month = Calendar.getInstance().get(Calendar.MONTH) + 1;
        }

        // 월의 시작과 끝
        LocalDateTime startDate = YearMonth.of(year, month).atDay(1).atStartOfDay();
        LocalDateTime endDate = YearMonth.of(year, month).atEndOfMonth().atTime(23, 59, 59);

        Map<String, LocalDateTime> startAndEndDate = new HashMap<>();
        startAndEndDate.put("startDate", startDate);
        startAndEndDate.put("endDate", endDate);

        return startAndEndDate;
    }

}
