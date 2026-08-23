package com.openrecordsmanager.list.dto;

import jakarta.validation.constraints.NotBlank;

public record UpdateListTypeRequest(
        @NotBlank String name
) {
}
