package com.sdi.notificationapi.service;

import com.sdi.notificationapi.dto.MemberInfoDto;
import com.sdi.notificationapi.dto.MessageDto;
import com.sdi.notificationapi.dto.request.RepairJigRequestDto;
import com.sdi.notificationapi.dto.request.RequestJigRequestDto;
import com.sdi.notificationapi.dto.request.ResponseJigRequestDto;
import com.sdi.notificationapi.dto.response.RepairJigDetailResponseDto;
import com.sdi.notificationapi.dto.response.RepairJigListResponseDto;
import com.sdi.notificationapi.dto.response.RequestJigDetailResponseDto;
import com.sdi.notificationapi.dto.response.RequestJigListResponseDto;
import com.sdi.notificationapi.entity.RepairRequestEntity;
import com.sdi.notificationapi.entity.WantRequestEntity;
import com.sdi.notificationapi.entity.WantResponseEntity;
import com.sdi.notificationapi.repository.RepairRequestsRepository;
import com.sdi.notificationapi.repository.WantRequestsRepository;
import com.sdi.notificationapi.repository.WantResponsesRepository;
import com.sdi.notificationapi.util.JigRequestStatus;
import com.sdi.notificationapi.util.MessageClient;
import com.sdi.notificationapi.util.SseStatus;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
public class RequestService {

    /*
     * 현재 API 게이트웨이에 알림서버의 api를 호출하는 메소드가 없으므로 연결 불가능
     */
    private final WantRequestsRepository wantRequestsRepository;
    private final WantResponsesRepository wantResponsesRepository;
    private final RepairRequestsRepository repairRequestsRepository;
    private final MessageClient messageClient;
//    private final JigItemClient jigItemClient;

    // Transactional 왜 적용이 안될까?
    // 알림 전송 실패하면 mongo에도 저장되면 안되는데..
    @Transactional
    public void createWantRequest(RequestJigRequestDto requestJigRequestDto, MemberInfoDto memberInfoDto, String accessToken) {
        /*
         * 1. 요청 내용 DB에 저장
         * 2. 알림 서버로 알림 전송 요청 보냄
         */
        if (requestJigRequestDto.serialNos() == null || requestJigRequestDto.serialNos().isEmpty()) {
            throw new IllegalArgumentException("불출 요청 리스트가 없습니다.");
        }
        WantRequestEntity content = wantRequestsRepository.save(WantRequestEntity.from(requestJigRequestDto, memberInfoDto.employeeNo()));
        messageClient.sendMessage(MessageDto.of(SseStatus.REQUEST_JIG, memberInfoDto.employeeNo(), content.getId()), accessToken);
    }

    @Transactional
    public void createWantResponse(ResponseJigRequestDto responseJigRequestDto, MemberInfoDto memberInfoDto, String accessToken) {
        /*
         * 1. 원본 요청의 상태를 FINISH로 변경
         * 2. 응답 내용 DB에 저장
         * 3. 알림 서버로 알림 전송 요청 보냄
         */

        WantRequestEntity originalRequest = wantRequestsRepository.findById(responseJigRequestDto.requestId())
                .orElseThrow(() -> new IllegalArgumentException("원본 요청을 찾을 수 없습니다."));
        originalRequest.updateWantRequest(memberInfoDto.employeeNo(), responseJigRequestDto.isAccept());
        wantRequestsRepository.save(originalRequest);
        WantResponseEntity content = wantResponsesRepository.save(WantResponseEntity.from(responseJigRequestDto));
        messageClient.sendMessage(MessageDto.of(SseStatus.RESPONSE_JIG, memberInfoDto.employeeNo(), content.getId()), accessToken);
    }

    @Transactional
    public void createRepairRequest(RepairJigRequestDto repairJigRequestDto, MemberInfoDto memberInfoDto, String accessToken) {
        /*
         * 1. 요청 내용 DB에 저장
         * 상태 변경은 다른 곳에서 하는거겠지?
         * 2. 알림 서버로 알림 전송 요청 보냄
         */
        RepairRequestEntity content = repairRequestsRepository.save(RepairRequestEntity.from(repairJigRequestDto, memberInfoDto.employeeNo()));
        messageClient.sendMessage(MessageDto.of(SseStatus.REQUEST_REPAIR, memberInfoDto.employeeNo(), content.getId()), accessToken);
    }

    public RequestJigListResponseDto findAllWantJigRequests(String option, int pageNumber, int size) {
        Pageable pageable = getPageable(pageNumber, size, "createdAt");
        Page<WantRequestEntity> page = switch (option) {
            case "PUBLISH", "FINISH" -> wantRequestsRepository.findAllByStatus(JigRequestStatus.valueOf(option), pageable);
            case "REJECT" -> wantRequestsRepository.findAllByIsAcceptAndStatus(false, JigRequestStatus.FINISH, pageable);
            case "", "ALL" -> wantRequestsRepository.findAll(pageable);
            default -> throw new IllegalStateException("Unexpected value: " + option);
        };

        List<RequestJigDetailResponseDto> dtoList = page.getContent().stream()
                .map(RequestJigDetailResponseDto::from)
                .collect(Collectors.toList());

        return RequestJigListResponseDto.of(page.getNumber() + 1, page.getTotalPages(), dtoList);
    }

    public RequestJigDetailResponseDto findOneJigRequest(String requestId) {
        return RequestJigDetailResponseDto.from(
                wantRequestsRepository.findById(requestId)
                        .orElseThrow(() -> new IllegalArgumentException("원본 요청을 찾을 수 없습니다.")));
    }

    public RepairJigListResponseDto findAllRepairRequests(int pageNumber, int size) {
        Pageable pageable = getPageable(pageNumber, size, "createdAt");
        Page<RepairRequestEntity> page = repairRequestsRepository.findAll(pageable);

        List<RepairJigDetailResponseDto> list = page.getContent().stream()
                .map(RepairJigDetailResponseDto::from)
                .collect(Collectors.toList());

        return RepairJigListResponseDto.of(page.getNumber() + 1, page.getTotalPages(), list);
    }

    private static PageRequest getPageable(int pageNumber, int size, String sortOption) {
        return PageRequest.of(Math.max(0, pageNumber - 1), Math.max(1, size), Sort.by(sortOption).descending());
    }

    public RepairJigDetailResponseDto findOneRepairRequest(String requestId) {
        return RepairJigDetailResponseDto.from(
                repairRequestsRepository.findById(requestId)
                        .orElseThrow(() -> new IllegalArgumentException("원본 요청을 찾을 수 없습니다."))
        );
    }
}
