package application;

import application.ports.EventStore;
import application.ports.QueuePublisher;
import domain.IncomingEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

public class IngestEventService {

    private static final Logger log = LoggerFactory.getLogger(IngestEventService.class);

    private final EventStore eventStore;
    private final QueuePublisher queuePublisher;

    public IngestEventService(EventStore eventStore, QueuePublisher queuePublisher) {
        this.eventStore = eventStore;
        this.queuePublisher = queuePublisher;
    }

    public String ingestEvent(String correlationId,
                              String source,
                              String producer,
                              String originalType,
                              String payloadJson
    ) {

        String eventId = UUID.randomUUID().toString();

        String resolvedCorrelationId = (correlationId == null || correlationId.isBlank())
                ? eventId
                : correlationId;

        IncomingEvent event = IncomingEvent.createNewEvent(
                eventId,
                resolvedCorrelationId,
                source,
                producer,
                originalType,
                payloadJson
        );

        eventStore.save(event);
        queuePublisher.publishEventId(eventId);

        log.info("event_ingested eventId={} correlationId={} source={} producer={} originalType={} status={}",
                event.getEventId(),
                event.getCorrelationId(),
                event.getSource(),
                event.getProducer(),
                event.getOriginalType(),
                event.getStatus());

        return eventId;
    }
}
