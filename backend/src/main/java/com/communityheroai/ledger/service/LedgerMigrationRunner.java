package com.communityheroai.ledger.service;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class LedgerMigrationRunner implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(LedgerMigrationRunner.class);
    private final CivicLedgerService ledgerService;

    @Override
    public void run(ApplicationArguments args) {
        int migrated = ledgerService.migrateLegacyEntries();
        if (migrated > 0) log.info("Migrated {} civic ledger entries to HMAC-SHA256", migrated);
    }
}
