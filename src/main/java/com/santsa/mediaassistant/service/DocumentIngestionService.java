package com.santsa.mediaassistant.service;

import org.springframework.core.io.Resource;

import com.santsa.mediaassistant.dto.IngestionResult;

public interface DocumentIngestionService {
    IngestionResult ingestNewDocument(Resource resource);
}
