package com.communityheroai.demo;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/demo")
@RequiredArgsConstructor
public class DemoSeedController {
    private final DemoDataSeeder seeder;

    @Value("${app.demo.seed-token:}")
    private String seedToken;

    @PostMapping("/seed")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> seed(
            @RequestParam(defaultValue = "false") boolean reset,
            @RequestHeader(value = "X-Demo-Seed-Token", required = false) String providedToken) {
        if (seedToken != null && !seedToken.isBlank() && !seedToken.equals(providedToken)) {
            return ResponseEntity.status(403).body(Map.of(
                    "seeded", false,
                    "message", "Invalid demo seed token"
            ));
        }
        int issueCount = seeder.seed(reset);
        return ResponseEntity.ok(Map.of(
                "seeded", issueCount > 0,
                "issueCount", issueCount,
                "reset", reset
        ));
    }
}
