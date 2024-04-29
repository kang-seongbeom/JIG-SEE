package com.sdi.apiserver.api.jig;

import com.sdi.apiserver.api.jig.client.JigItemClient;
import com.sdi.apiserver.api.jig.dto.request.JigItemAddRequestDto;
import com.sdi.apiserver.api.jig.dto.request.JigItemExchangeRequestDto;
import com.sdi.apiserver.api.jig.dto.request.JigItemSerialNoRequestDto;
import com.sdi.apiserver.api.jig.dto.request.JigItemUpdateStatusRequestDto;
import com.sdi.apiserver.api.jig.dto.response.JigItemIsUsableResponseDto;
import com.sdi.apiserver.api.jig.dto.response.JigItemResponseDto;
import com.sdi.apiserver.util.Response;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/jig-item")
@RequiredArgsConstructor
@Slf4j
class JigItemController {

    private final JigItemClient jigItemClient;

    @GetMapping
    Response<JigItemResponseDto> findBySerialNo(@RequestParam(name = "serial-no") String serialNo) {
        log.info("{}로 Jig-Item정보 조회", serialNo);
        return jigItemClient.findBySerialNo(serialNo);
    }

    @PostMapping
    Response<Void> add(@RequestBody JigItemAddRequestDto dto) {
        log.info("Jig-Item 데이터 삽입{}", dto.list());
        return jigItemClient.add(dto);
    }

    @GetMapping("/usable")
    Response<JigItemIsUsableResponseDto> isUsable(@RequestParam(name = "facility-model") String facilityModel,
                                                  @RequestParam(name = "jig-serial-no") String jigSerialNo) {
        log.info("{}에 {}가 들어갈 수 있는지 확인", facilityModel, jigSerialNo);
        return jigItemClient.isUsable(facilityModel, jigSerialNo);
    }

    @DeleteMapping()
    Response<Void> delete(@RequestBody JigItemSerialNoRequestDto dto) {
        log.info("{} 삭제", dto.serialNo());
        return jigItemClient.delete(dto);
    }

    @PutMapping("/status")
    Response<Void> updateStatus(@RequestBody JigItemUpdateStatusRequestDto dto){
        log.info("{} 상태 변경 {}", dto.serialNo(), dto.status());
        return jigItemClient.updateStatus(dto);
    }

    @PutMapping("/exchange")
    Response<Void> exchange(@RequestBody JigItemExchangeRequestDto dto){
        log.info("{}에 {}를 {}로 변경", dto.facilitySerialNo(), dto.beforeSerialNo(), dto.afterSerialNo());
        return jigItemClient.exchange(dto);
    }

    @PutMapping("/recovery")
    Response<Void> recovery(@RequestBody JigItemSerialNoRequestDto dto) {
        log.info("{} 상태 변경", dto.serialNo());
        return jigItemClient.recovery(dto);
    }
}