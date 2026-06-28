package com.communityheroai.issue.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class IssueVerificationRequest {
    @NotBlank
    @Size(max = 100)
    private String verifierName;
    @Email
    @Size(max = 254)
    private String verifierEmail;
    @Size(max = 1000)
    private String comment;
}
