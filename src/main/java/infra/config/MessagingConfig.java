package infra.config;

import application.ports.QueuePublisher;
import application.worker.EventProcessingWorker;
import infra.sqs.InMemoryQueuePublisher;
import infra.sqs.SqsEventConsumer;
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

    @Bean
    @Profile("aws")
    public SqsEventConsumer sqsEventConsumer(
            SqsClient sqsClient,
            @Value("${app.aws.sqs.queue-url}") String queueUrl,
            EventProcessingWorker eventProcessingWorker
    ) {
        return new SqsEventConsumer(sqsClient, queueUrl, eventProcessingWorker);
    }
}