package com.sdi.apiserver.api.facility.dto.response;

import java.util.List;


public record FacilityDetailResponseDto(
        Long id,
        String model,
        String facilitySerialNo,
        List<JigItemSummary> list

) {


    public record JigItemSummary(
            Long id,
            String model,
            String serialNo,
            String status,
            String expectLife,
            Integer useCount,
            String useAccumulationTime,
            Integer repairCount

    ) {
    }
}
