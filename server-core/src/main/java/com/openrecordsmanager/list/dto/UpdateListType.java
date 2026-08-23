package com.openrecordsmanager.list.dto;

import jakarta.validation.constraints.NotBlank;

public record UpdateListType(
        @NotBlank String name
) {
}
