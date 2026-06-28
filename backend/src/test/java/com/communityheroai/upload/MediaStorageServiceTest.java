package com.communityheroai.upload;

import com.communityheroai.issue.entity.IssueMediaType;
import com.google.cloud.storage.Blob;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class MediaStorageServiceTest {
    @Test
    void storesSupportedImageInCloudStorage() {
        Storage storage = mock(Storage.class);
        MediaStorageService service = new MediaStorageService("evidence-bucket", "evidence", storage);
        byte[] bytes = {1, 2, 3};
        MockMultipartFile file = new MockMultipartFile(
                "files", "pothole.jpg", "image/jpeg", bytes);

        MediaStorageService.StoredMedia stored = service.store(file);

        assertThat(stored.type()).isEqualTo(IssueMediaType.IMAGE);
        assertThat(stored.storageKey()).endsWith(".jpg");
        verify(storage).create(any(BlobInfo.class), any(byte[].class),
                any(Storage.BlobTargetOption[].class));
    }

    @Test
    void readsStoredCloudObject() {
        Storage storage = mock(Storage.class);
        Blob blob = mock(Blob.class);
        when(blob.exists()).thenReturn(true);
        when(blob.getContentType()).thenReturn("image/png");
        when(blob.getContent()).thenReturn(new byte[]{4, 5, 6});
        when(storage.get(BlobId.of("evidence-bucket", "evidence/photo.png"))).thenReturn(blob);
        MediaStorageService service = new MediaStorageService("evidence-bucket", "evidence", storage);

        MediaStorageService.StoredObject stored = service.read("photo.png");

        assertThat(stored.contentType()).isEqualTo("image/png");
        assertThat(stored.bytes()).containsExactly(4, 5, 6);
    }

    @Test
    void rejectsUnsupportedMediaType() {
        MediaStorageService service = new MediaStorageService(
                "evidence-bucket", "evidence", mock(Storage.class));
        MockMultipartFile file = new MockMultipartFile(
                "files", "payload.html", "text/html", "unsafe".getBytes());

        assertThatThrownBy(() -> service.store(file))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("supported");
    }
}
