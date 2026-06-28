package com.communityheroai.ledger.service;

import com.communityheroai.ledger.dto.LedgerIntegrityResponse;
import com.communityheroai.ledger.entity.CivicLedgerEntry;
import com.communityheroai.ledger.repository.CivicLedgerRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HexFormat;
import java.util.List;

@Service
public class CivicLedgerService {
    public static final String GENESIS_HASH = "0".repeat(64);
    public static final String LEGACY_SHA256 = "SHA256";
    public static final String HMAC_SHA256 = "HMAC_SHA256";

    private final CivicLedgerRepository ledgerRepository;
    private final byte[] hmacSecret;

    public CivicLedgerService(CivicLedgerRepository ledgerRepository,
                              @Value("${app.ledger.hmac-secret}") String hmacSecret) {
        this.ledgerRepository = ledgerRepository;
        if (hmacSecret == null || hmacSecret.length() < 32) {
            throw new IllegalStateException("LEDGER_HMAC_SECRET must contain at least 32 characters.");
        }
        this.hmacSecret = hmacSecret.getBytes(StandardCharsets.UTF_8);
    }

    @Transactional
    public synchronized CivicLedgerEntry append(String eventType, String aggregateType, Long aggregateId,
                                                 String actorName, String payload) {
        String previousHash = ledgerRepository.findTopByOrderByIdDesc()
                .map(CivicLedgerEntry::getEntryHash)
                .orElse(GENESIS_HASH);
        LocalDateTime createdAt = normalizedTimestamp(LocalDateTime.now());
        String safeActor = blankToDefault(actorName, "Community Hero AI");
        String safePayload = blankToDefault(payload, "{}");
        String entryHash = calculate(HMAC_SHA256,
                canonical(eventType, aggregateType, aggregateId, safeActor, safePayload, previousHash, createdAt));
        return ledgerRepository.save(CivicLedgerEntry.builder()
                .eventType(eventType)
                .aggregateType(aggregateType)
                .aggregateId(aggregateId)
                .actorName(safeActor)
                .payload(safePayload)
                .previousHash(previousHash)
                .entryHash(entryHash)
                .hashAlgorithm(HMAC_SHA256)
                .createdAt(createdAt)
                .build());
    }

    @Transactional(readOnly = true)
    public LedgerIntegrityResponse verify() {
        return verifyEntries(ledgerRepository.findAllByOrderByIdAsc());
    }

    @Transactional
    public int migrateLegacyEntries() {
        List<CivicLedgerEntry> entries = ledgerRepository.findAllByOrderByIdAsc();
        LedgerIntegrityResponse current = verifyEntries(entries);
        if (!current.isValid()) {
            throw new IllegalStateException("Refusing to sign a compromised civic ledger: " + current.getMessage());
        }
        boolean migrationRequired = entries.stream()
                .anyMatch(entry -> !HMAC_SHA256.equals(entry.getHashAlgorithm()));
        if (!migrationRequired) return 0;

        String previousHash = GENESIS_HASH;
        for (CivicLedgerEntry entry : entries) {
            entry.setPreviousHash(previousHash);
            entry.setHashAlgorithm(HMAC_SHA256);
            entry.setEntryHash(calculate(HMAC_SHA256,
                    canonical(entry.getEventType(), entry.getAggregateType(), entry.getAggregateId(),
                            entry.getActorName(), entry.getPayload(), previousHash, entry.getCreatedAt())));
            previousHash = entry.getEntryHash();
        }
        ledgerRepository.saveAll(entries);
        return entries.size();
    }

    private LedgerIntegrityResponse verifyEntries(List<CivicLedgerEntry> entries) {
        String expectedPrevious = GENESIS_HASH;
        for (CivicLedgerEntry entry : entries) {
            if (!secureEquals(expectedPrevious, entry.getPreviousHash())) {
                return compromised(entry, entries.size(),
                        "Audit log compromised: previous hash mismatch at ledger entry #" + entry.getId() + ".");
            }
            String algorithm = blankToDefault(entry.getHashAlgorithm(), LEGACY_SHA256);
            String recalculated = calculate(algorithm,
                    canonical(entry.getEventType(), entry.getAggregateType(), entry.getAggregateId(),
                            entry.getActorName(), entry.getPayload(), entry.getPreviousHash(), entry.getCreatedAt()));
            if (!secureEquals(recalculated, entry.getEntryHash())) {
                return compromised(entry, entries.size(),
                        "Audit log compromised: entry hash mismatch at ledger entry #" + entry.getId() + ".");
            }
            expectedPrevious = entry.getEntryHash();
        }
        return LedgerIntegrityResponse.builder()
                .valid(true)
                .totalEntries(entries.size())
                .lastHash(expectedPrevious)
                .message(entries.isEmpty()
                        ? "Ledger is empty. New civic actions will start a signed chain."
                        : "System integrity verified. Civic ledger HMAC chain is intact.")
                .build();
    }

    private LedgerIntegrityResponse compromised(CivicLedgerEntry entry, int totalEntries, String message) {
        return LedgerIntegrityResponse.builder()
                .valid(false)
                .totalEntries(totalEntries)
                .compromisedEntryId(entry.getId())
                .lastHash(entry.getEntryHash())
                .message(message)
                .build();
    }

    private String calculate(String algorithm, String canonical) {
        return HMAC_SHA256.equals(algorithm) ? hmac(canonical) : sha256(canonical);
    }

    private String hmac(String value) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(hmacSecret, "HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal((HMAC_SHA256 + "|" + value)
                    .getBytes(StandardCharsets.UTF_8)));
        } catch (Exception ex) {
            throw new IllegalStateException("HMAC-SHA256 hashing is unavailable", ex);
        }
    }

    private String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception ex) {
            throw new IllegalStateException("SHA-256 hashing is unavailable", ex);
        }
    }

    private boolean secureEquals(String left, String right) {
        if (left == null || right == null) return false;
        return MessageDigest.isEqual(left.getBytes(StandardCharsets.UTF_8), right.getBytes(StandardCharsets.UTF_8));
    }

    private String canonical(String eventType, String aggregateType, Long aggregateId,
                             String actorName, String payload, String previousHash, LocalDateTime createdAt) {
        return String.join("|",
                blankToDefault(eventType, "UNKNOWN"),
                blankToDefault(aggregateType, "UNKNOWN"),
                String.valueOf(aggregateId),
                blankToDefault(actorName, "Community Hero AI"),
                blankToDefault(payload, "{}"),
                blankToDefault(previousHash, GENESIS_HASH),
                createdAt == null ? "" : createdAt.toString());
    }

    private LocalDateTime normalizedTimestamp(LocalDateTime value) {
        return value == null ? null : value.truncatedTo(ChronoUnit.SECONDS);
    }

    private String blankToDefault(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }
}
