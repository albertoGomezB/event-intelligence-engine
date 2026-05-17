package application.service;

import api.EventResponse;
import application.ports.EventStore;
import domain.IncomingEvent;

import java.util.Optional;

public class GetEventService {

    private final EventStore eventStore;

    public GetEventService(EventStore eventStore) {
        this.eventStore = eventStore;
    }

    public Optional<EventResponse> findById(String eventId) {
        return eventStore.findById(eventId)
                .map(this::toResponse);
    }

    private EventResponse toResponse(IncomingEvent event) {
        return new EventResponse(
                event.getEventId(),
                event.getCorrelationId(),
                event.getSource(),
                event.getProducer(),
                event.getOriginalType(),
                event.getStatus(),
                event.getAttempts(),
                event.getCreatedAt(),
                event.getUpdatedAt(),
                event.getClassification()
        );
    }
}
