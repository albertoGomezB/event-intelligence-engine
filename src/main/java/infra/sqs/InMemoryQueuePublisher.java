package infra.sqs;

import application.ports.QueuePublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public class InMemoryQueuePublisher implements QueuePublisher {

    private final Queue<String> queue  = new ConcurrentLinkedQueue<>();
    private static final Logger log =
            LoggerFactory.getLogger(InMemoryQueuePublisher.class);

    @Override
    public void publishEventId(String eventId) {
        queue.add(eventId);
        log.info("Published event to the queue eventId={}", eventId);
    }
}
