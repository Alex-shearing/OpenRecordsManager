package com.openrecordsmanager.setup;

import com.openrecordsmanager.rest.swagger.ConflictApiResponse;
import com.openrecordsmanager.rest.swagger.InternalServerErrorApiResponse;
import com.openrecordsmanager.rest.swagger.ValidationFailedApiResponse;
import com.openrecordsmanager.setup.dto.CompleteSetupRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/setup")
@InternalServerErrorApiResponse
@ApiResponse(responseCode = "200")
public class SetupController {

    private final SetupService setupService;

    public SetupController(SetupService setupService) {
        this.setupService = setupService;
    }

    @PostMapping(value = "/complete", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Complete first-run setup with an admin account")
    @ConflictApiResponse
    @ValidationFailedApiResponse
    public void completeSetup(@Valid @RequestBody CompleteSetupRequest request) {
        this.setupService.complete(request);
    }
}
