package com.openrecordsmanager.controllers;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/file_stores")
public class FileStoreController {

    public FileStoreController() {
    }

    @GetMapping()
    public ApiResponse<String> get() {
        return ApiResponse.success("ok");
    }

}
