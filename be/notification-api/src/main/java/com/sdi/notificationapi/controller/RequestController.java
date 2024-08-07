package com.sdi.notificationapi.controller;

import com.sdi.notificationapi.dto.MemberInfoDto;
import com.sdi.notificationapi.dto.request.RepairJigRequestDto;
import com.sdi.notificationapi.dto.request.RequestJigRequestDto;
import com.sdi.notificationapi.dto.request.ResponseJigRequestDto;
import com.sdi.notificationapi.dto.response.RepairJigDetailResponseDto;
import com.sdi.notificationapi.dto.response.RepairJigListResponseDto;
import com.sdi.notificationapi.dto.response.RequestJigDetailResponseDto;
import com.sdi.notificationapi.dto.response.RequestJigListResponseDto;
import com.sdi.notificationapi.service.ApiService;
import com.sdi.notificationapi.service.RequestService;
import com.sdi.notificationapi.util.Response;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1")
@RequiredArgsConstructor
class RequestController {
    private final RequestService requestService;
    private final ApiService apiService;

    @PostMapping("/request/jig")
    Response<Void> makeWantRequest(@RequestBody RequestJigRequestDto requestJigRequestDto, @RequestHeader("Authorization") String accessToken) {
        MemberInfoDto memberInfoDto = apiService.getCurrentMember(accessToken);
        requestService.createWantRequest(requestJigRequestDto, memberInfoDto, accessToken);
        return Response.success();
    }

    @PostMapping("/response/jig")
    Response<Void> makeWantResponse(@RequestBody ResponseJigRequestDto responseJigRequestDto, @RequestHeader("Authorization") String accessToken) {
        MemberInfoDto memberInfoDto = apiService.getCurrentMember(accessToken);
        requestService.createWantResponse(responseJigRequestDto, memberInfoDto, accessToken);
        return Response.success();
    }

    @PostMapping("/request/repair")
    Response<Void> makeRepairRequest(@RequestBody RepairJigRequestDto repairJigRequestDto, @RequestHeader("Authorization") String accessToken) {
        MemberInfoDto memberInfoDto = apiService.getCurrentMember(accessToken);
        requestService.createRepairRequest(repairJigRequestDto, memberInfoDto, accessToken);
        return Response.success();
    }

    @GetMapping("/request/jig/all")
    Response<RequestJigListResponseDto> findAllWantJigRequests(@RequestParam(value = "filter",defaultValue = "ALL") String option,
                                                               @RequestParam(value = "page", defaultValue = "1") int page,
                                                               @RequestParam(value = "size", defaultValue = "10") int size) {
        return Response.success(requestService.findAllWantJigRequests(option, page, size));
    }

    @GetMapping("/request/jig/detail")
    Response<RequestJigDetailResponseDto> findOneJigRequest(@RequestParam(value = "request-jig-id") String requestId) {
        return Response.success(requestService.findOneJigRequest(requestId));
    }

    @GetMapping("/request/repair")
    Response<RepairJigListResponseDto> findAllRepairRequests(@RequestParam(value = "page", defaultValue = "1") int page,
                                                             @RequestParam(value = "size", defaultValue = "10") int size) {
        return Response.success(requestService.findAllRepairRequests(page, size));
    }

    @GetMapping("/request/repair/detail")
    Response<RepairJigDetailResponseDto> findOneRepairRequest(@RequestParam(value = "repair-jig-id") String requestId) {
        return Response.success(requestService.findOneRepairRequest(requestId));
    }
}
