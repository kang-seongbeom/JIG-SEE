package com.sdi.apiserver.api.jig.dto.request;

import java.util.List;

public record JigItemInspectionRequestDto(
        List<String> serialNos
) {
}