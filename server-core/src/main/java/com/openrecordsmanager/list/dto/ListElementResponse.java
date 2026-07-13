package com.openrecordsmanager.list.dto;

import com.openrecordsmanager.api.ResourceIdentifier;
import com.openrecordsmanager.list.ListElement;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.Date;
import java.util.Set;

public record ListElementResponse(
        @NotBlank ResourceIdentifier type,
        @NotBlank String name,
        @NotBlank String description,
        @NotNull Set<String> aliases,
        @NotNull int index,
        @NotNull Date activeTo) {


    public static ListElementResponse from(ListElement element) {
        return new ListElementResponse(
                element.id,
                element.name,
                element.description,
                element.aliases,
                element.elementIndex,
                element.activeTo
        );
    }
}
