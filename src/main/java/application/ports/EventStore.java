package application.ports;

import domain.IncomingEvent;

import java.util.Optional;

public interface EventStore {

    void save(IncomingEvent event);

    Optional<IncomingEvent> findById(String eventId);

    void update(IncomingEvent event);
}
