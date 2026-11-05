package com.santsa.mediaassistant.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.santsa.mediaassistant.dto.IngestionResult;
import com.santsa.mediaassistant.service.DocumentIngestionService;

import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;

@RestController
@RequestMapping("/api/v1/documents")
@RequiredArgsConstructor
public class DocumentController {

    private final DocumentIngestionService documentIngestionService;

    @PostMapping("/ingest")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<IngestionResult> ingestDocument(
            @RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(documentIngestionService.ingestNewDocument(file.getResource()));
    }

}
