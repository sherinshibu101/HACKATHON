package com.communityheroai.upload;

import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.TimeUnit;

@RestController
@RequiredArgsConstructor
public class StoredMediaController {
    private final MediaStorageService storageService;

    @GetMapping("/uploads/{storageKey:.+}")
    public ResponseEntity<byte[]> get(@PathVariable String storageKey) {
        MediaStorageService.StoredObject stored = storageService.read(storageKey);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(stored.contentType()))
                .cacheControl(CacheControl.maxAge(1, TimeUnit.HOURS).cachePrivate())
                .body(stored.bytes());
    }
}
