package com.sdi.work_order.application;

import com.sdi.work_order.client.response.JigItemResponseDto;
import com.sdi.work_order.dto.reponse.WorkOrderDetailResponseDto;
import com.sdi.work_order.dto.reponse.WorkOrderGroupingResponseDto;
import com.sdi.work_order.dto.reponse.WorkOrderResponseDto;
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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import static com.sdi.work_order.dto.request.WorkOrderUpdateStatusRequestDto.UpdateStatusItem;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class WorkOrderService {

    private final JigItemService jigItemService;
    private final MemberService memberService;
    private final WorkOrderRDBRepository workOrderRDBRepository;
    private final WorkOrderNosqlRepository workOrderNosqlRepository;
    private final WorkOrderCriteriaRepository workOrderCriteriaRepository;

    public WorkOrderDetailResponseDto detail(Long workOrderId, String accessToken) {
        WorkOrderRDBEntity rdb = getRDBWorkOrderById(workOrderId);
        JigItemResponseDto jigItem = getJigItem(rdb.getJigSerialNo());
        WorkOrderNosqlEntity nosql = getNosqlWorkOrderCheckList(rdb.getCheckListId());

        String creator = getMemberInfo(rdb.getCreatorEmployeeNo(), accessToken);
        String terminator = getMemberInfo(rdb.getTerminatorEmployeeNo(), accessToken);

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
        if (employeeNo != null) {
            Page<WorkOrderRDBEntity> infos = workOrderRDBRepository.findAllByCreatorEmployeeNoOrderByCreatedAtDesc(employeeNo, pageable);
            return mapToWorkOrderResponseDto(infos, mapToWorkOrderItems(accessToken, infos));
        } else if (name != null) {
            List<String> memberEmployeeNos = memberService.getMemberEmployeeByNames(accessToken, name);
            Page<WorkOrderRDBEntity> infos = workOrderCriteriaRepository.findByMembers(memberEmployeeNos, pageable);
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
        JigItemResponseDto jigItem = getJigItem(dto.serialNo());

        // 같은 Jig Item에 대해서 이미 생성된 WO가 있다면 생성 불가
        isAlreadyExistNotFinishWorkOrderThenThrow(jigItem);

        // 저장할 데이터 생성
        String checkListId = UUID.randomUUID().toString();
        WorkOrderRDBEntity rdb = WorkOrderRDBEntity.from(employeeNo, jigItem.serialNo(), jigItem.model(), checkListId);
        WorkOrderNosqlEntity nosql = WorkOrderNosqlEntity.from(checkListId, false, WorkOrderCheckItem.from(jigItem.checkList()));

        // 데이터 저장
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

    @Transactional
    public void tmpSave(Long id, List<WorkOrderCheckItem> updateCheckList) {
        WorkOrderRDBEntity rdb = getRDBWorkOrderById(id);
        saveData(rdb, updateCheckList);
    }

    @Transactional
    public void save(String accessToken, Long id, List<WorkOrderCheckItem> checkList) {
        String terminatorEmployeeNo = getMemberEmployeeNo(accessToken);

        WorkOrderRDBEntity rdb = getRDBWorkOrderById(id);
        saveData(rdb, checkList);
        rdb.updateTerminatorEmployeeNo(terminatorEmployeeNo);
    }

    @Transactional
    public void updateStatus(List<UpdateStatusItem> list) {
        for (UpdateStatusItem item : list) {
            WorkOrderRDBEntity workOrder = getRDBWorkOrderById(item.id());
            if (workOrder.getStatus() == WorkOrderStatus.FINISH) continue; // 이미 종료된 wo는 수정 불가

            workOrder.updateStatus(item.status());
        }
    }

    private Pageable getPageable(int page, int size) {
        return PageRequest.of(Math.max(0, page - 1), Math.max(1, size));
    }

    private WorkOrderResponseDto mapToWorkOrderResponseDto(Page<WorkOrderRDBEntity> infos, List<WorkOrderItem> workOrderItems) {
        return WorkOrderResponseDto.of(infos.getNumber() + 1, infos.getTotalPages(), workOrderItems);
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

    private void saveData(WorkOrderRDBEntity rdb, List<WorkOrderCheckItem> updateCheckList) {
        WorkOrderNosqlEntity nosql;

        if (rdb.getCheckListId() != null) {
            nosql = getNosqlWorkOrderCheckList(rdb.getCheckListId());
            nosql.updateCheckList(updateCheckList);
        } else {
            String uuid = UUID.randomUUID().toString();
            nosql = WorkOrderNosqlEntity.from(uuid, false, updateCheckList);
            rdb.updatedCheckListId(uuid);
        }
        workOrderNosqlRepository.save(nosql);
        rdb.updateDate();
    }

    private JigItemResponseDto getJigItem(String serialNo) {
        return jigItemService.findBySerialNo(serialNo).getResult();
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
}