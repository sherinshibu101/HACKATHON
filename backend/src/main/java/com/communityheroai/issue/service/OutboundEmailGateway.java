package com.communityheroai.issue.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class OutboundEmailGateway {
    private final RestClient resendClient;
    private final boolean enabled;
    private final String resendApiKey;
    private final String resendFrom;

    public OutboundEmailGateway(
            RestClient.Builder restClientBuilder,
            @Value("${app.email.enabled:false}") boolean enabled,
            @Value("${app.email.resend-api-key:}") String resendApiKey,
            @Value("${app.email.resend-from:}") String resendFrom,
            @Value("${app.email.resend-base-url:https://api.resend.com}") String resendBaseUrl) {
        this.resendClient = restClientBuilder.baseUrl(resendBaseUrl).build();
        this.enabled = enabled;
        this.resendApiKey = resendApiKey;
        this.resendFrom = resendFrom;
    }

    public boolean isConfigured() {
        return enabled && notBlank(resendApiKey) && notBlank(resendFrom);
    }

    public String configurationWarning() {
        if (!enabled) return "Email delivery is disabled. Set EMAIL_SENDING_ENABLED=true after configuring a provider.";
        return "Resend is not configured. Add RESEND_API_KEY and RESEND_FROM.";
    }

    public void send(OutboundEmailMessage outbound) {
        if (!isConfigured()) throw new IllegalStateException(configurationWarning());
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("from", resendFrom);
        payload.put("to", List.of(outbound.recipient()));
        payload.put("subject", outbound.subject());
        payload.put("text", outbound.body());
        if (notBlank(outbound.replyTo())) payload.put("reply_to", outbound.replyTo().trim());
        resendClient.post()
                .uri("/emails")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + resendApiKey)
                .body(payload)
                .retrieve()
                .toBodilessEntity();
    }

    private boolean notBlank(String value) {
        return value != null && !value.isBlank();
    }
}
