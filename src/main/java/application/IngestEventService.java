package application;

import application.ports.EventStore;
import application.ports.QueuePublisher;
import domain.EventStatus;
import domain.IncomingEvent;

import java.time.Instant;
import java.util.UUID;

public class IngestEventService {

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
        var now = Instant.now();

       String resolvedCorrelationId = (correlationId == null || correlationId.isBlank())
               ? eventId
               : correlationId;

        IncomingEvent event = new IncomingEvent(
                eventId,
                resolvedCorrelationId,
                source,
                producer,
                originalType,
                payloadJson,
                EventStatus.RECEIVED,
                0,
                now,
                now,
                null
        );

        eventStore.save(event);
        queuePublisher.publishEventId(eventId);

        return eventId;
    }
}
