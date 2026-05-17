package infra.config;

import application.ports.EventClassifier;
import infra.ai.MockEventClassifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AiConfig {

    @Bean
    public EventClassifier eventClassifier() {
        return new MockEventClassifier();
    }
}