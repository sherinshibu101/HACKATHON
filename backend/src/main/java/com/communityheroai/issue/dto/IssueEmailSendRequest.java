package com.communityheroai.issue.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record IssueEmailSendRequest(
        @NotBlank @Size(max = 254) String recipient,
        @NotBlank @Size(max = 255) String subject,
        @NotBlank @Size(max = 10000) String body,
        @AssertTrue(message = "Email content must be explicitly confirmed") boolean confirmed
) {
}
