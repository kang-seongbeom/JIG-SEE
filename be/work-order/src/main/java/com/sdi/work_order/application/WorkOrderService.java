package com.sdi.work_order.application;

import com.sdi.work_order.client.JigItemClient;
import com.sdi.work_order.client.response.JigItemResponseDto;
import com.sdi.work_order.client.response.MemberListResponseDto;
import com.sdi.work_order.client.response.MemberResponseDto;
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

import java.util.*;
import java.util.stream.Collectors;

import static com.sdi.work_order.dto.request.WorkOrderUpdateStatusRequestDto.UpdateStatusItem;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class WorkOrderService {

    private final JigItemClient jigItemClient;
    private final WorkOrderRDBRepository workOrderRDBRepository;
    private final WorkOrderNosqlRepository workOrderNosqlRepository;
    private final WorkOrderCriteriaRepository workOrderCriteriaRepository;

    public WorkOrderDetailResponseDto detail(Long workOrderId) {
        WorkOrderRDBEntity rdb = getRDBWorkOrderById(workOrderId);
        JigItemResponseDto jigItem = getJigItem(rdb.getJigSerialNo());
        WorkOrderNosqlEntity nosql = getNosqlWorkOrderCheckList(rdb.getCheckListId());

        // TODO: 생성자, 완료자 조회
        String creator = rdb.getCreatorEmployeeNo();
        String terminator = rdb.getTerminatorEmployeeNo(); // null 확인 필요

        return WorkOrderDetailResponseDto.from(rdb, creator, terminator, jigItem, nosql.getCheckList());
    }

    public WorkOrderResponseDto all(WorkOrderStatus status, int page, int size) {
        Pageable pageable = getPageable(page, size);

        Page<WorkOrderRDBEntity> infos = switch (status) {
            case PUBLISH, PROGRESS, FINISH -> workOrderRDBRepository.findByStatusOrderByCreatedAtDesc(status, pageable);
            case null -> workOrderRDBRepository.findAllByOrderByCreatedAtDesc(pageable);
        };
        return mapToWorkOrderResponseDto(infos, mapToWorkOrderItems(infos));
    }

    public WorkOrderGroupingResponseDto grouping() {
        List<WorkOrderRDBEntity> all = workOrderRDBRepository.findAll();
        List<WorkOrderItem> items = mapToWorkOrderItems(all);

        Map<WorkOrderStatus, List<WorkOrderItem>> group =
                items.stream()
                        .collect(Collectors.groupingBy(WorkOrderItem::status));
        return WorkOrderGroupingResponseDto.from(group);
    }

    public WorkOrderResponseDto findByPerson(String employeeNo, String name, int page, int size) {
        Pageable pageable = getPageable(page, size);
        if (employeeNo != null) {
            Page<WorkOrderRDBEntity> infos = workOrderRDBRepository.findAllByCreatorEmployeeNoOrderByCreatedAtDesc(employeeNo, pageable);
            return mapToWorkOrderResponseDto(infos, mapToWorkOrderItems(infos));
        } else if (name != null) {
            // TODO: 사람 이름에 맞는 사용자 검색
            MemberListResponseDto members = MemberListResponseDto.from(List.of(MemberResponseDto.of(name, "creator1"), MemberResponseDto.of(name, "creator2")));
            List<String> memberEmployeeNos = members.list().stream()
                    .map(MemberResponseDto::employeeNo)
                    .toList();
            Page<WorkOrderRDBEntity> infos = workOrderCriteriaRepository.findByMembers(memberEmployeeNos, pageable);
            return mapToWorkOrderResponseDto(infos, mapToWorkOrderItems(infos));
        }

        return all(null, page, size);
    }

    @Transactional
    public void create(WorkOrderCreateRequestDto dto) {
        // 사번 조회
        // TODO: 사용자 연결 완료 시 실 사번으로 대체
        String employeeNo = "생성자 사번2";

        // 일련번호로 jig 조회
        JigItemResponseDto jigItem = getJigItem(dto.serialNo());

        // 저장할 데이터 생성
        String checkListId = UUID.randomUUID().toString();
        WorkOrderRDBEntity rdb = WorkOrderRDBEntity.from(employeeNo, jigItem.serialNo(), jigItem.model(), checkListId);
        WorkOrderNosqlEntity nosql = WorkOrderNosqlEntity.from(checkListId, false, WorkOrderCheckItem.from(jigItem.checkList()));

        // 데이터 저장
        workOrderRDBRepository.save(rdb);
        workOrderNosqlRepository.save(nosql);
    }

    @Transactional
    public void tmpSave(Long id, List<WorkOrderCheckItem> updateCheckList) {
        WorkOrderRDBEntity rdb = getRDBWorkOrderById(id);
        saveData(rdb, updateCheckList);
    }

    @Transactional
    public void save(Long id, List<WorkOrderCheckItem> checkList) {
        // TODO: 사용자 사번 조회
        String terminatorEmployeeNo = "완료자";

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

    private static Pageable getPageable(int page, int size) {
        return PageRequest.of(Math.max(0, page - 1), Math.max(1, size));
    }

    private static WorkOrderResponseDto mapToWorkOrderResponseDto(Page<WorkOrderRDBEntity> infos, List<WorkOrderItem> workOrderItems) {
        return WorkOrderResponseDto.of(infos.getNumber() + 1, infos.getTotalPages(), workOrderItems);
    }

    // List와 Page의 공통 상위 객체인 Iterable 사용
    private List<WorkOrderItem> mapToWorkOrderItems(Iterable<WorkOrderRDBEntity> infos) {
        List<WorkOrderItem> items = new ArrayList<>();

        for (WorkOrderRDBEntity info : infos) {
            // TODO: 사용자 연결 완료시 실제 사용자 이름으로 대체
            String creator = info.getCreatorEmployeeNo();
            String terminator = info.getTerminatorEmployeeNo();

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
        return jigItemClient.findBySerialNo(serialNo).getResult();
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