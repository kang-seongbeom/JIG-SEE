package com.sdi.common.dto;

public record JigRepairRequestDto(
        String serialNo, // Jig 시리얼 넘버
        String memo
) {
}
