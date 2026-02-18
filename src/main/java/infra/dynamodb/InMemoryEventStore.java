package infra.dynamodb;

import application.ports.EventStore;
import domain.IncomingEvent;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryEventStore implements EventStore {

    private final Map<String, IncomingEvent> storage = new ConcurrentHashMap<>();


    @Override
    public void save(IncomingEvent event) {
        storage.put(event.eventId(), event);
    }

    @Override
    public Optional<IncomingEvent> findById(String eventId) {
        return Optional.ofNullable(storage.get(eventId));
    }

    @Override
    public void update(IncomingEvent event) {
        storage.put(event.eventId(), event);
    }
}
