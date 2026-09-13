package com.openrecordsmanager.plugin.authlocal;

import com.openrecordsmanager.api.user.UserActionContext;
import com.openrecordsmanager.api.user.UserActionType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import org.mindrot.jbcrypt.BCrypt;

public class ResetLocalPasswordAction extends UserActionType<ResetLocalPasswordAction.Inputs> {

    public ResetLocalPasswordAction() {
        super(Inputs.class, "Reset Local Password", "Set a new password for local authentication");
    }

    public record Inputs(
            @Schema(title = "New Password", format = "password", accessMode = Schema.AccessMode.WRITE_ONLY)
            @NotBlank String newPassword
    ) {
    }

    @Override
    public boolean isAvailable(UserActionContext context) {
        return context.isPropertyRegistered(AuthLocalPlugin.PASSWORD_HASH_PROPERTY);
    }

    @Override
    public void execute(UserActionContext context, Inputs inputs) {
        String hash = BCrypt.hashpw(inputs.newPassword(), BCrypt.gensalt());
        context.setTargetProperty(AuthLocalPlugin.PASSWORD_HASH_PROPERTY, hash);
    }
}
