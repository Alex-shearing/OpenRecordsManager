package com.openrecordsmanager.plugin.authlocal;

import com.openrecordsmanager.api.location.LocationActionContext;
import com.openrecordsmanager.api.location.LocationActionType;
import com.openrecordsmanager.api.location.LocationKind;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import org.mindrot.jbcrypt.BCrypt;

public class ResetLocalPasswordAction extends LocationActionType<ResetLocalPasswordAction.Inputs> {

    public ResetLocalPasswordAction() {
        super(Inputs.class);
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
    public boolean isAvailable(LocationActionContext context) {
        return context.getTargetKind() == LocationKind.USER
                && context.isPropertyRegistered(AuthLocalPlugin.PASSWORD_HASH_PROPERTY);
    }

    @Override
    public void execute(LocationActionContext context, Inputs inputs) {
        String hash = BCrypt.hashpw(inputs.newPassword(), BCrypt.gensalt());
        context.setTargetProperty(AuthLocalPlugin.PASSWORD_HASH_PROPERTY, hash);
    }
}
