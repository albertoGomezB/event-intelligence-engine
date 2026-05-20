package infra.config;

import application.ports.EventClassifier;
import com.fasterxml.jackson.databind.ObjectMapper;
import infra.ai.MockEventClassifier;
import infra.ai.bedrock.BedrockClassificationPromptBuilder;
import infra.ai.bedrock.BedrockClassificationResponseParser;
import infra.ai.bedrock.BedrockEventClassifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeClient;

@Configuration
public class AiConfig {

    @Bean
    @Profile("!bedrock")
    public EventClassifier mockEventClassifier() {
        return new MockEventClassifier();
    }

    @Bean
    @Profile("bedrock")
    public BedrockClassificationPromptBuilder bedrockClassificationPromptBuilder() {
        return new BedrockClassificationPromptBuilder();
    }

    @Bean
    @Profile("bedrock")
    public BedrockClassificationResponseParser bedrockClassificationResponseParser(ObjectMapper objectMapper) {
        return new BedrockClassificationResponseParser(objectMapper);
    }

    @Bean
    @Profile("bedrock")
    public EventClassifier bedrockEventClassifier(
            BedrockRuntimeClient bedrockRuntimeClient,
            BedrockClassificationPromptBuilder promptBuilder,
            BedrockClassificationResponseParser responseParser,
            @Value("${app.aws.bedrock.model-id}") String modelId,
            @Value("${app.ai.prompt-version}") String promptVersion,
            @Value("${app.ai.classifier.max-tokens}") int maxTokens,
            @Value("${app.ai.classifier.temperature}") float temperature
    ) {
        return new BedrockEventClassifier(
                bedrockRuntimeClient,
                promptBuilder,
                responseParser,
                modelId,
                promptVersion,
                maxTokens,
                temperature
        );
    }
}