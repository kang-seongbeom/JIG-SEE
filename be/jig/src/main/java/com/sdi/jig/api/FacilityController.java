package com.sdi.jig.api;

import com.sdi.jig.application.FacilityItemService;
import com.sdi.jig.dto.response.FacilityItemAllResponseDto;
import com.sdi.jig.util.Response;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/facility-item")
@RequiredArgsConstructor
public class FacilityController {

    private final FacilityItemService facilityItemService;

    @GetMapping("/all")
    Response<FacilityItemAllResponseDto> all(){
        FacilityItemAllResponseDto dto = facilityItemService.all();
        return Response.success(dto);
    }
}
