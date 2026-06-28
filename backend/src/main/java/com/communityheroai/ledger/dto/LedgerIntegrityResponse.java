package com.communityheroai.ledger.dto;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class LedgerIntegrityResponse {
    boolean valid;
    long totalEntries;
    Long compromisedEntryId;
    String lastHash;
    String message;
}
