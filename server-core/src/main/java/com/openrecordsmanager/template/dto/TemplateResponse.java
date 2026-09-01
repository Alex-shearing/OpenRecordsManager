package com.openrecordsmanager.template.dto;

import com.openrecordsmanager.api.ResourceIdentifier;
import com.openrecordsmanager.api.template.TemplateComponent;
import jakarta.validation.constraints.NotBlank;

public record TemplateResponse(@NotBlank ResourceIdentifier id, @NotBlank String name) {
    public static TemplateResponse of(ResourceIdentifier id, TemplateComponent template) {
        return new TemplateResponse(id, template.name());
    }
}
