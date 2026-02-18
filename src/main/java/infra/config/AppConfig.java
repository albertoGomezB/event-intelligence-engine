package infra.config;

import application.IngestEventService;
import application.ports.EventStore;
import application.ports.QueuePublisher;
import infra.dynamodb.InMemoryEventStore;
import infra.sqs.InMemoryQueuePublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AppConfig {

    @Bean
    public EventStore eventStore () {
        return new InMemoryEventStore();
    }

    @Bean
    public QueuePublisher queuePublisher () {
        return new InMemoryQueuePublisher();
    }

    @Bean
    public IngestEventService ingestEventService (EventStore eventStore,
                                                  QueuePublisher queuePublisher) {
        return new IngestEventService(eventStore, queuePublisher);
    }
}
