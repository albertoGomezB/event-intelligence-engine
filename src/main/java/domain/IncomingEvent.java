package domain;

import lombok.Getter;

import java.time.Instant;

@Getter
public class IncomingEvent {

    private String eventId;
    private String correlationId;
    private String source;
    private String producer;
    private String originalType;
    private String payloadJson;
    private EventStatus status;
    private int attempts;
    private Instant createdAt;
    private Instant updatedAt;
    private ClassificationResult classification;

    private IncomingEvent(String eventId, String correlationId, String source, String producer,
                          String originalType, String payloadJson, EventStatus status,
                          int attempts, Instant createdAt, Instant updatedAt,
                          ClassificationResult classification) {
        this.eventId = eventId;
        this.correlationId = correlationId;
        this.source = source;
        this.producer = producer;
        this.originalType = originalType;
        this.payloadJson = payloadJson;
        this.status = status;
        this.attempts = attempts;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.classification = classification;
    }

    public static IncomingEvent createNewEvent(
            String eventId,
            String correlationId,
            String source,
            String producer,
            String originalType,
            String payloadJson) {

        if(eventId == null || eventId.isBlank()) {
            throw new IllegalArgumentException("Event ID cannot be null or blank");
        }
        if (payloadJson == null) {
            throw new IllegalArgumentException("Payload JSON cannot be null");
        }
        Instant now = Instant.now();

        return new IncomingEvent(
                eventId,
                correlationId,
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
    }

    public static IncomingEvent restore(
            String eventId,
            String correlationId,
            String source,
            String producer,
            String originalType,
            String payloadJson,
            EventStatus status,
            int attempts,
            Instant createdAt,
            Instant updatedAt,
            ClassificationResult classification) {

        if(eventId == null || eventId.isBlank()) {
            throw new IllegalArgumentException("Event ID cannot be null or blank");
        }
        if (payloadJson == null) {
            throw new IllegalArgumentException("Payload JSON cannot be null");
        }
        if (status == null) {
            throw new IllegalArgumentException("Event status cannot be null");
        }
        if (attempts < 0) {
            throw new IllegalArgumentException("Attempts cannot be negative");
        }
        if (createdAt == null) {
            throw new IllegalArgumentException("Created timestamp cannot be null");
        }
        if (updatedAt == null) {
            throw new IllegalArgumentException("Updated timestamp cannot be null");
        }
        if ((status == EventStatus.COMPLETED || status == EventStatus.REVIEW_REQUIRED) && classification == null) {
            throw new IllegalArgumentException("Classification result is required for classified terminal events");
        }

        return new IncomingEvent(
                eventId,
                correlationId,
                source,
                producer,
                originalType,
                payloadJson,
                status,
                attempts,
                createdAt,
                updatedAt,
                classification
        );
    }

    /**
     * Transition the event status to PROCESSING.
     * This method should only be called when the event is in RECEIVED status.
     * The status will be updated to PROCESSING after this method is called.
     */
    public void startProcessing() {
        if (this.status == EventStatus.COMPLETED ||
                this.status == EventStatus.FAILED ||
                this.status == EventStatus.REVIEW_REQUIRED) {
            throw new IllegalStateException("Cannot process terminal event");
        }
        status = EventStatus.PROCESSING;
        updatedAt = Instant.now();
    }

    /**
     * Complete the event with the classification result.
     * This method should only be called when the event is in PROCESSING status.
     * The status will be updated to COMPLETED after this method is called.
     * @param result the classification result to associate with the event
     */
    public void complete (ClassificationResult result) {
        if(result == null) {
            throw new IllegalArgumentException("The result cannot be null");
        }
        if(this.status != EventStatus.PROCESSING) {
            throw new IllegalStateException("The status should be PROCESSING");
        }
        this.classification = result;
        updatedAt = Instant.now();
        this.status = EventStatus.COMPLETED;
    }

    /**
     * Mark the event for manual review when automatic classification should not be trusted.
     * This stores the AI classification result for traceability and transitions the event to
     * REVIEW_REQUIRED status.
     *
     * @param result classification output produced by the classifier
     */
    public void markForReview(ClassificationResult result) {
        if(result == null) {
            throw new IllegalArgumentException("The result cannot be null");
        }
        if (this.status != EventStatus.PROCESSING) {
            throw new IllegalStateException("The status should be PROCESSING");
        }
        this.classification = result;
        updatedAt = Instant.now();
        this.status = EventStatus.REVIEW_REQUIRED;
    }

    /**
     * Register a failure for the event processing. This method should only be called when the event is in PROCESSING status.
     * The status will be updated to FAILED if the number of attempts exceeds the maxRetries.
     * The attempts count will be incremented by 1 after this method is called.
     * The status will remain PROCESSING if the number of attempts does not exceed the maxRetries.
     * The updatedAt timestamp will be updated to the current time after this method is called.
     * @param maxRetries the maximum number of retries allowed before marking the event as FAILED
     *
     * @return true if the event is marked as FAILED, false if the event remains in PROCESSING
     */
    public boolean registerFailure (int maxRetries) {

        if (this.status != EventStatus.PROCESSING) {
            throw new IllegalStateException("Cannot register failure if event is not PROCESSING");
        }
        this.attempts++;
        this.updatedAt = Instant.now();

        if (this.attempts >= maxRetries) {
            this.status = EventStatus.FAILED;
            return false;
        }
        return true;
    }

}
