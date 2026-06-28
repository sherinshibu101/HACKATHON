package com.communityheroai.agent.dto;

public record DispatchAnalysis(
    String proposedDepartment,
    String proposedPriority,
    String draftedCitizenNotification
) {
}