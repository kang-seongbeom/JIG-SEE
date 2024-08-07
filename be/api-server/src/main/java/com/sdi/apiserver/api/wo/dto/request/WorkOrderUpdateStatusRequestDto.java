package com.sdi.apiserver.api.wo.dto.request;

import java.util.List;

public record WorkOrderUpdateStatusRequestDto(
        List<UpdateStatus> list
) {
    public record UpdateStatus(Long id, String status) {
    }
}
