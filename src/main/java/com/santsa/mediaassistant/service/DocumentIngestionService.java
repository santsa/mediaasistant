package com.santsa.mediaassistant.service;

import org.springframework.core.io.Resource;

public interface DocumentIngestionService {
    String ingestNewDocument(Resource resource);
}
