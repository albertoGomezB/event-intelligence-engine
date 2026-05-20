package application.worker;

import application.ports.ClassificationPolicy;
import application.ports.EventClassifier;
import application.ports.EventStore;
import application.ports.QueuePublisher;
import domain.ClassificationResponse;
import domain.ClassificationResult;
import domain.ClassificationRequest;
import domain.EventStatus;
import domain.IncomingEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

/**
 * EventProcessingWorker is responsible for processing incoming events by calling an AI classifier.
 * The worker is designed to handle non-deterministic behavior of the AI classifier, ensuring id
 * impotency and resilience through a retry mechanism for temporary failures.
 */
public class EventProcessingWorker {

    private static final int MAX_RETRIES = 3;
    private static final Logger log = LoggerFactory.getLogger(EventProcessingWorker.class);

    private final EventStore eventStore;
    private final EventClassifier classifier;
    private final QueuePublisher queuePublisher;
    private final ClassificationPolicy classificationPolicy;

    public EventProcessingWorker(EventStore eventStore, EventClassifier classifier, QueuePublisher queuePublisher, ClassificationPolicy classificationPolicy) {
        this.eventStore = eventStore;
        this.classifier = classifier;
        this.queuePublisher = queuePublisher;
        this.classificationPolicy = classificationPolicy;
    }

    /**
     * Processes an event by its ID. It retrieves the event from the store, checks if it has already been processed,
     * and if not, it attempts to classify the event using the AI classifier. The method
     * handles retries for temporary failures and updates the event status accordingly.
     *
     * @param eventId the ID of the event to process
     *
     */
    public void process(String eventId) {

        // 1. Load event from persistence
        Optional<IncomingEvent> optionalEvent = eventStore.findById(eventId);
        if (optionalEvent.isEmpty()) {
            log.warn("event_not_found eventId={}", eventId);
            return;
        }
        IncomingEvent event = optionalEvent.get();

        // 2. Idempotency guard:
        // If event already reached a terminal state, ignore duplicated delivery.
        if (event.getStatus() == EventStatus.COMPLETED ||
                event.getStatus() == EventStatus.FAILED ||
                event.getStatus() == EventStatus.REVIEW_REQUIRED) {
            log.info("event_ignored_terminal_state eventId={} correlationId={} status={}",
                    event.getEventId(), event.getCorrelationId(), event.getStatus());
            return;
        }

        // 3. Transition to PROCESSING before calling external dependency
        event.startProcessing();
        eventStore.save(event);
        log.info("event_processing_started eventId={} correlationId={} attempts={} status={}",
                event.getEventId(), event.getCorrelationId(), event.getAttempts(), event.getStatus());

        // 4. Call AI classifier (non-deterministic dependency)
        ClassificationRequest classificationRequest = new ClassificationRequest(
                event.getSource(),
                event.getProducer(),
                event.getOriginalType(),
                event.getPayloadJson()
        );

        ClassificationResponse classificationResponse = classifier.classify(classificationRequest);

        // 5. Successful classification
        if (classificationResponse.success()) {
            ClassificationResult result = classificationResponse.result();
            if(result == null) {
                event.registerFailure(1);
                eventStore.save(event);
                log.error("event_invalid_classifier_response eventId={} correlationId={} " +
                                "reason=SUCCESS_WITHOUT_RESULT status={}",
                        event.getEventId(), event.getCorrelationId(), event.getStatus());
                return;
            }
            boolean classificationNotAllowed = !classificationPolicy.isClassificationAllowed(
                    result.category(),
                    result.subcategory());

            boolean lowConfidence = classificationPolicy.requiresHumanReview(result.confidence());

            if (classificationNotAllowed || lowConfidence) {
                String reason = classificationNotAllowed
                        ? "CLASSIFICATION_NOT_ALLOWED"
                        : "LOW_CONFIDENCE";

                event.markForReview(result);
                eventStore.save(event);

                log.info("event_review_required eventId={} correlationId={} reason={} category={} subcategory={} confidence={} status={}",
                        event.getEventId(),
                        event.getCorrelationId(),
                        reason,
                        result.category(),
                        result.subcategory(),
                        result.confidence(),
                        event.getStatus());

                return;
            }
            event.complete(result);
            eventStore.save(event);
            log.info("event_completed eventId={} correlationId={} category={} subcategory={} confidence={} status={}",
                    event.getEventId(),
                    event.getCorrelationId(),
                    result.category(),
                    result.subcategory(),
                    result.confidence(),
                    event.getStatus());

            return;
        }

        // 6. Temporary failure
        if (classificationResponse.temporaryFailure()) {
            boolean retry = event.registerFailure(MAX_RETRIES);
            eventStore.save(event);
            log.warn("event_temporary_failure eventId={} correlationId={} attempts={} willRetry={} error={}",
                    event.getEventId(), event.getCorrelationId(), event.getAttempts(), retry,
                    classificationResponse.errorMessage());
            if(retry) {
                queuePublisher.publishEventId(eventId);
            }
            return;
        }
        // 7. Permanent failure (invalid response, unrecoverable error)
        if (classificationResponse.permanentFailure()) {
            event.registerFailure(1);
            eventStore.save(event);
            log.error("event_permanent_failure eventId={} correlationId={} attempts={} error={} status={}",
                    event.getEventId(), event.getCorrelationId(), event.getAttempts(),
                    classificationResponse.errorMessage(), event.getStatus());
        }
    }
}
