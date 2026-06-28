package com.communityheroai.issue.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record EmergencyEscalationRequest(
        @NotBlank @Size(max = 100) String requesterName,
        @Email @Size(max = 254) String requesterEmail,
        @NotBlank @Size(max = 1000) String reason,
        boolean overrideAiAssessment
) {
}
