package com.communityheroai.issue.service;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class OutboundEmailGatewayTest {
    @Test
    void resendProviderSendsAuthenticatedMessageWithCitizenReplyTo() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        OutboundEmailGateway gateway = new OutboundEmailGateway(
                builder, true, "re_test_key",
                "Community Hero AI <onboarding@resend.dev>", "https://api.resend.test");

        server.expect(once(), requestTo("https://api.resend.test/emails"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer re_test_key"))
                .andExpect(jsonPath("$.from").value("Community Hero AI <onboarding@resend.dev>"))
                .andExpect(jsonPath("$.to[0]").value("admin@example.com"))
                .andExpect(jsonPath("$.subject").value("Urgent civic issue"))
                .andExpect(jsonPath("$.text").value("Please review this issue."))
                .andExpect(jsonPath("$.reply_to").value("citizen@example.com"))
                .andRespond(withSuccess());

        gateway.send(new OutboundEmailMessage(
                "admin@example.com", "Urgent civic issue",
                "Please review this issue.", "citizen@example.com"));

        server.verify();
    }
}
