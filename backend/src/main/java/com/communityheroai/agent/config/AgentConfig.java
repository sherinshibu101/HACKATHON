package com.communityheroai.agent.config;

import com.communityheroai.agent.service.DispatchAgent;
import dev.langchain4j.model.chat.Capability;
import dev.langchain4j.model.googleai.GoogleAiGeminiChatModel;
import dev.langchain4j.service.AiServices;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AgentConfig {

    @Value("${gemini.api-key}")
    private String geminiApiKey;

    @Value("${gemini.model:gemini-2.5-flash}")
    private String geminiModel;

    @Bean
    public DispatchAgent dispatchAgent() {
        GoogleAiGeminiChatModel model = GoogleAiGeminiChatModel.builder()
            .apiKey(geminiApiKey)
            .modelName(geminiModel)
            .build();

        return AiServices.builder(DispatchAgent.class)
            .chatLanguageModel(model)
            .build();
    }
}