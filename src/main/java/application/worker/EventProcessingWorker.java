package application.worker;

import application.ports.EventClassifier;
import application.ports.EventStore;
import application.ports.QueuePublisher;
import domain.ClassificationResponse;
import domain.EventStatus;
import domain.IncomingEvent;

import java.util.Optional;

/**
 * EventProcessingWorker is responsible for processing incoming events by calling an AI classifier.
 * The worker is designed to handle non-deterministic behavior of the AI classifier, ensuring id
 * impotency and resilience through a retry mechanism for temporary failures.
 */
public class EventProcessingWorker {

    private static final  int MAX_RETRIES = 3;

    private final EventStore eventStore;
    private final EventClassifier classifier;
    private final QueuePublisher queuePublisher;

    public EventProcessingWorker(EventStore eventStore, EventClassifier classifier, QueuePublisher queuePublisher) {
        this.eventStore = eventStore;
        this.classifier = classifier;
        this.queuePublisher = queuePublisher;
    }

    /**
     * Processes a single event identified by eventId.
     * The method is designed to be idempotent and resilient to non-deterministic behavior of the AI classifier.
     * The processing flow includes:
     * 1. Loading the event from persistence.
     * 2. Checking for idempotency (ignoring already completed or failed events).
     * 3. Transitioning the event to PROCESSING state before calling the AI classifier.
     * 4. Handling the AI classifier response:
     *   - On success: transition to COMPLETED and save the result.
     *   - On temporary failure: register the failure, and if retries are available, re-queue the event for another attempt.
     *   - On permanent failure: transition to FAILED.
     */
    public void process(String eventId) {

        // 1. Load event from persistence
        Optional<IncomingEvent> optionalEvent = eventStore.findById(eventId);
        if (optionalEvent.isEmpty()) {
            return;
        }
        IncomingEvent event = optionalEvent.get();

        // 2. Idempotency guard:
        // If event already reached a terminal state, ignore duplicated delivery.
        if (event.getStatus() == EventStatus.COMPLETED ||
                event.getStatus() == EventStatus.FAILED) {
            return;
        }

        // 3. Transition to PROCESSING before calling external dependency
        event.startProcessing();
        eventStore.save(event);

        // 4. Call AI classifier (non-deterministic dependency)
        ClassificationResponse classificationResponse = classifier.classify(event.getPayloadJson());

        // 5. Successful classification
        if (classificationResponse.isSuccess()) {
            event.complete(classificationResponse.getResult());
            eventStore.save(event);
            return;
        }

        // 6. Temporary failure
        if (classificationResponse.isTemporaryFailure()) {
            boolean retry = event.registerFailure(MAX_RETRIES);
            eventStore.save(event);
            if(retry) {
                // Re-queue event for another processing attempt
                queuePublisher.publishEventId(eventId);
            }
            return;
        }
        // 7. Permanent failure (invalid response, unrecoverable error)
        if (classificationResponse.isPermanentFailure()) {
            event.registerFailure(1); // forces FAILED state
            eventStore.save(event);
        }
    }
}
