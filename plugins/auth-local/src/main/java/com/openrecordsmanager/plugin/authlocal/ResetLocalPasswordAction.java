package com.openrecordsmanager.plugin.authlocal;

import com.openrecordsmanager.api.ComponentReference;
import com.openrecordsmanager.api.action.ActionContext;
import com.openrecordsmanager.api.location.LocationActionType;
import com.openrecordsmanager.api.property.ManagedObjectKind;
import com.openrecordsmanager.api.template.property.ObjectPropertyTemplate;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import org.mindrot.jbcrypt.BCrypt;

public class ResetLocalPasswordAction extends LocationActionType<ResetLocalPasswordAction.Inputs> {

    private final ComponentReference<ObjectPropertyTemplate<String>> passwordProperty;

    public ResetLocalPasswordAction(ComponentReference<ObjectPropertyTemplate<String>> passwordProperty) {
        super(Inputs.class);
        this.passwordProperty = passwordProperty;
    }

    public record Inputs(
            @Schema(
                    title = "location_action.auth_local.reset_password.schema.newPassword.title",
                    format = "password",
                    accessMode = Schema.AccessMode.WRITE_ONLY
            )
            @NotBlank String newPassword
    ) {
    }

    @Override
    public boolean isAvailable(ActionContext context) {
        return context.getTargetKind() == ManagedObjectKind.USER
                && context.isPropertyRegistered(this.passwordProperty);
    }

    @Override
    public void execute(ActionContext context, Inputs inputs) {
        String hash = BCrypt.hashpw(inputs.newPassword(), BCrypt.gensalt());
        context.setTargetProperty(this.passwordProperty, hash);
    }
}
