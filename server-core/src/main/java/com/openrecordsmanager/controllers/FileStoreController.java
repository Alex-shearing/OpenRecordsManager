package com.openrecordsmanager.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/file_store")
public class FileStoreController {

    public FileStoreController() {
    }

    @GetMapping()
    public ResponseEntity<ApiResponse<String>> get() {
        return ResponseEntity.ok(ApiResponse.success("ok"));
    }

}
