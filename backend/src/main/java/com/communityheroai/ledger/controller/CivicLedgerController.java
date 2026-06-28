package com.communityheroai.ledger.controller;

import com.communityheroai.ledger.dto.LedgerIntegrityResponse;
import com.communityheroai.ledger.service.CivicLedgerService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/ledger")
@RequiredArgsConstructor
@CrossOrigin
public class CivicLedgerController {
    private final CivicLedgerService ledgerService;

    @GetMapping("/integrity")
    @PreAuthorize("hasRole('ADMIN')")
    public LedgerIntegrityResponse verifyIntegrity() {
        return ledgerService.verify();
    }
}
