package com.communityheroai.issue.service;

public record OutboundEmailMessage(
        String recipient,
        String subject,
        String body,
        String replyTo
) {
}
