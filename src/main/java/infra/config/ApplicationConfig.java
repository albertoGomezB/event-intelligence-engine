package infra.config;

import application.ports.ClassificationPolicy;
import application.ports.EventClassifier;
import application.ports.EventStore;
import application.ports.QueuePublisher;
import application.service.GetEventService;
import application.service.IngestEventService;
import application.worker.EventProcessingWorker;
import infra.policy.BankingClassificationPolicy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ApplicationConfig {

    @Bean
    public IngestEventService ingestEventService(EventStore eventStore,
                                                 QueuePublisher queuePublisher) {
        return new IngestEventService(eventStore, queuePublisher);
    }

    @Bean
    public GetEventService getEventService(EventStore eventStore) {
        return new GetEventService(eventStore);
    }

    @Bean
    public ClassificationPolicy classificationPolicy() {
        return new BankingClassificationPolicy();
    }

    @Bean
    public EventProcessingWorker eventProcessingWorker(EventStore eventStore,
                                                       EventClassifier eventClassifier,
                                                       QueuePublisher queuePublisher,
                                                       ClassificationPolicy classificationPolicy) {
        return new EventProcessingWorker(
                eventStore,
                eventClassifier,
                queuePublisher,
                classificationPolicy
        );
    }
}