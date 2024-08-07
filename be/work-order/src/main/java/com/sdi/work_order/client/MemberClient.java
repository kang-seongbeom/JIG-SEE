package com.sdi.work_order.client;

import com.sdi.work_order.client.response.MemberResponseDto;
import com.sdi.work_order.util.Response;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@FeignClient(name = "memberClient", url = "${apis.api-base-url}")
public interface MemberClient {

    String ACCESS_TOKEN_PREFIX = "Authorization";
    String EMPLOYEE_NO = "employee-no";
    String NAME = "name";

    @GetMapping("/member/search")
    Response<MemberResponseDto> findMemberByToken(@RequestHeader(name = ACCESS_TOKEN_PREFIX) String accessToken);


    @GetMapping("/member/search/employee-no")
    Response<MemberResponseDto> findMemberByEmployeeNo(@RequestHeader(name = ACCESS_TOKEN_PREFIX) String accessToken,
                                                       @RequestParam(name = EMPLOYEE_NO) String employeeNo);

    @GetMapping("/member/search/name")
    Response<List<MemberResponseDto>> findMemberByName(@RequestHeader(name = ACCESS_TOKEN_PREFIX) String accessToken,
                                                       @RequestParam(name = NAME) String name);
}
