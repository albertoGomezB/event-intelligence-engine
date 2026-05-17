package infra.config;

import application.ports.QueuePublisher;
import infra.sqs.InMemoryQueuePublisher;
import infra.sqs.SqsQueuePublisher;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import software.amazon.awssdk.services.sqs.SqsClient;

@Configuration
public class MessagingConfig {

    @Bean
    @Profile("aws")
    public QueuePublisher sqsQueuePublisher(SqsClient sqsClient,
                                            @Value("${app.aws.sqs.queue-url}") String queueUrl) {
        return new SqsQueuePublisher(sqsClient, queueUrl);
    }

    @Bean
    @Profile("!aws")
    public QueuePublisher inMemoryQueuePublisher() {
        return new InMemoryQueuePublisher();
    }
}