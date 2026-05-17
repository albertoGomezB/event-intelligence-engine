package infra.config;

import application.IngestEventService;
import application.ports.ClassificationPolicy;
import application.ports.EventStore;
import application.ports.QueuePublisher;
import infra.dynamodb.DynamoDbEventStore;
import infra.dynamodb.InMemoryEventStore;
import infra.policy.BankingClassificationPolicy;
import infra.sqs.InMemoryQueuePublisher;
import infra.sqs.SqsQueuePublisher;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.sqs.SqsClient;

@Configuration
public class AppConfig {


    @Bean
    @Profile("aws")
    public DynamoDbClient dynamoDbClient() {
        return DynamoDbClient.create();
    }

    @Bean
    @Profile("aws")
    public SqsClient sqsClient() {
        return SqsClient.create();
    }

    @Bean
    @Profile("aws")
    public EventStore dynamoEventStore(DynamoDbClient dynamoDbClient,
                                       @Value("${app.aws.dynamodb.table-name}") String tableName) {
        return new DynamoDbEventStore(dynamoDbClient, tableName);
    }

    @Bean
    @Profile("aws")
    public QueuePublisher sqsQueuePublisher(SqsClient sqsClient,
                                            @Value("${app.aws.sqs.queue-url}") String queueUrl) {
        return new SqsQueuePublisher(sqsClient, queueUrl);
    }

    @Bean
    @Profile("!aws")
    public EventStore eventStore () {
        return new InMemoryEventStore();
    }

    @Bean
    @Profile("!aws")
    public QueuePublisher queuePublisher () {
        return new InMemoryQueuePublisher();
    }

    @Bean
    public IngestEventService ingestEventService (EventStore eventStore,
                                                  QueuePublisher queuePublisher) {
        return new IngestEventService(eventStore, queuePublisher);
    }

    @Bean
    public ClassificationPolicy classificationPolicy() {
        return new BankingClassificationPolicy();
    }
}