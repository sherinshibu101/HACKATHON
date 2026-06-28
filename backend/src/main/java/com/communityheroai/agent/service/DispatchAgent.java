package com.communityheroai.agent.service;

import com.communityheroai.agent.dto.DispatchAnalysis;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

public interface DispatchAgent {

    @SystemMessage("""
        You are an internal Admin Portal advisor. Your role is to analyze incoming civic issue reports
        and generate structured draft recommendations for human review. You do not take autonomous actions.
        Every output you produce will be reviewed by an admin before any decision is made.

        Output strictly as JSON matching the DispatchAnalysis schema:
        - proposedDepartment: The civic department best suited to handle this issue (e.g., "Roads & Infrastructure", "Water & Sanitation", "Electrical Services", "Waste Management", "Parks & Recreation")
        - proposedPriority: Priority level as "LOW", "MEDIUM", "HIGH", or "CRITICAL"
        - draftedCitizenNotification: A professional, empathetic draft message to the citizen acknowledging their report and setting expectations
        """)
    @UserMessage("""
        Analyze this civic issue report:
        Description: {{description}}
        Vision AI Labels: {{visionLabels}}
        Category: {{category}}
        Ward: {{ward}}

        Provide your structured analysis for admin review.
        """)
    DispatchAnalysis analyzeTicket(
        @V("description") String description,
        @V("visionLabels") String visionLabels,
        @V("category") String category,
        @V("ward") String ward
    );

    static DispatchAgent create() {
        return AiServices.builder(DispatchAgent.class).build();
    }
}