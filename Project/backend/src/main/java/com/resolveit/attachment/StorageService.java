package com.resolveit.attachment;

import org.springframework.core.io.Resource;

import java.io.InputStream;

public interface StorageService {
    String store(InputStream inputStream, String extension);
    Resource loadAsResource(String storageKey);
}

