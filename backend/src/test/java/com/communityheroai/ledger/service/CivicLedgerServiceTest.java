package com.communityheroai.ledger.service;

import com.communityheroai.ledger.entity.CivicLedgerEntry;
import com.communityheroai.ledger.repository.CivicLedgerRepository;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class CivicLedgerServiceTest {
    private static final String SECRET = "a-secure-ledger-test-secret-with-32-characters";

    @Test
    void newEntriesUseHmacSha256() {
        CivicLedgerRepository repository = mock(CivicLedgerRepository.class);
        when(repository.findTopByOrderByIdDesc()).thenReturn(Optional.empty());
        when(repository.save(any(CivicLedgerEntry.class))).thenAnswer(invocation -> invocation.getArgument(0));
        CivicLedgerService service = new CivicLedgerService(repository, SECRET);

        CivicLedgerEntry entry = service.append(
                "STATUS_CHANGED", "ISSUE", 4L, "Admin", "{\"status\":\"RESOLVED\"}");

        assertThat(entry.getHashAlgorithm()).isEqualTo(CivicLedgerService.HMAC_SHA256);
        assertThat(entry.getPreviousHash()).isEqualTo(CivicLedgerService.GENESIS_HASH);
        assertThat(entry.getEntryHash()).hasSize(64);
    }

    @Test
    void validLegacyChainMigratesAndRemainsVerifiable() throws Exception {
        CivicLedgerRepository repository = mock(CivicLedgerRepository.class);
        LocalDateTime createdAt = LocalDateTime.of(2026, 6, 1, 10, 30, 0);
        String canonical = String.join("|", "ISSUE_REPORTED", "ISSUE", "1", "Citizen", "{}",
                CivicLedgerService.GENESIS_HASH, createdAt.toString());
        CivicLedgerEntry legacy = CivicLedgerEntry.builder()
                .id(1L)
                .eventType("ISSUE_REPORTED")
                .aggregateType("ISSUE")
                .aggregateId(1L)
                .actorName("Citizen")
                .payload("{}")
                .previousHash(CivicLedgerService.GENESIS_HASH)
                .entryHash(sha256(canonical))
                .createdAt(createdAt)
                .build();
        when(repository.findAllByOrderByIdAsc()).thenReturn(List.of(legacy));
        CivicLedgerService service = new CivicLedgerService(repository, SECRET);

        int migrated = service.migrateLegacyEntries();

        assertThat(migrated).isEqualTo(1);
        assertThat(legacy.getHashAlgorithm()).isEqualTo(CivicLedgerService.HMAC_SHA256);
        assertThat(legacy.getEntryHash()).isNotEqualTo(sha256(canonical));
        assertThat(service.verify().isValid()).isTrue();
        verify(repository).saveAll(List.of(legacy));
    }

    private String sha256(String value) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                .digest(value.getBytes(StandardCharsets.UTF_8)));
    }
}
