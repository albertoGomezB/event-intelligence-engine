package infra.config;

import application.ports.EventStore;
import infra.dynamodb.DynamoDbEventStore;
import infra.dynamodb.InMemoryEventStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

@Configuration
public class PersistenceConfig {

    @Bean
    @Profile("aws")
    public EventStore dynamoEventStore(DynamoDbClient dynamoDbClient,
                                       @Value("${app.aws.dynamodb.table-name}") String tableName) {
        return new DynamoDbEventStore(dynamoDbClient, tableName);
    }

    @Bean
    @Profile("!aws")
    public EventStore inMemoryEventStore() {
        return new InMemoryEventStore();
    }
}