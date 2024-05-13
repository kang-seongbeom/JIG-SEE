package com.sdi.jig.api;

import com.sdi.jig.application.JigService;
import com.sdi.jig.dto.request.JigUpdateRequestDto;
import com.sdi.jig.dto.response.JigModelCountResponseDto;
import com.sdi.jig.dto.response.JigMonthResponseDto;
import com.sdi.jig.dto.response.JigResponseDto;
import com.sdi.jig.dto.response.JigUpdatedCheckListResponseDto;
import com.sdi.jig.util.JigStatus;
import com.sdi.jig.util.Response;
import com.sdi.jig.util.TokenHeader;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/jig")
@RequiredArgsConstructor
class JigController {

    private final JigService jigService;

    @PutMapping
    Response<Void> updateCheckList(@RequestBody JigUpdateRequestDto dto) {
        jigService.updateCheckList(dto.model(), dto.checkList());
        return Response.success();
    }

    @GetMapping
    Response<JigResponseDto> findByModel(@RequestParam(name = "model") String model) {
        return Response.success(jigService.findByModel(model));
    }

    @GetMapping("/status")
    Response<JigMonthResponseDto> monthStatus(HttpServletRequest request,
                                              @RequestParam(name = "year", required = false) Integer year,
                                              @RequestParam(name = "month", required = false) Integer month) {
        String accessToken = request.getHeader(TokenHeader.AUTHORIZATION);
        return Response.success(jigService.monthStatus(accessToken, year, month));
    }

    @GetMapping("/count")
    Response<JigModelCountResponseDto> jigCountStatus(@RequestParam(name = "status") JigStatus status) {
        return Response.success(jigService.jigCountStatus(status));
    }

    @GetMapping("/update-check-list")
    Response<JigUpdatedCheckListResponseDto> updatedCheckList(@RequestParam(name = "year", required = false) Integer year,
                                                              @RequestParam(name = "month", required = false) Integer month) {
        return Response.success(jigService.updatedCheckList(year, month));
    }
}
