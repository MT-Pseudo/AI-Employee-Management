package com.company.ems.config;

import com.company.ems.ai.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.*;

@Configuration
public class AiConfig {

    @Bean
    @ConditionalOnProperty(name = "app.ai.provider", havingValue = "openai")
    public NarrativeGenerator openAiNarrativeGenerator(
            @Value("${app.ai.openai.api-key:}") String apiKey,
            @Value("${app.ai.openai.model:gpt-4o-mini}") String model) {
        return new OpenAiNarrativeGenerator(new RuleBasedNarrativeGenerator(), apiKey, model);
    }

    @Bean
    @ConditionalOnProperty(name = "app.ai.provider", havingValue = "rule-based", matchIfMissing = true)
    public NarrativeGenerator ruleBasedNarrativeGenerator() {
        return new RuleBasedNarrativeGenerator();
    }
}