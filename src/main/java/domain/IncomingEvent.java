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

    /**
     * Transition the event status to PROCESSING.
     * This method should only be called when the event is in RECEIVED status.
     * The status will be updated to PROCESSING after this method is called.
     */
    public void startProcessing() {
        if (this.status == EventStatus.COMPLETED ||
                this.status == EventStatus.FAILED) {
            throw new IllegalStateException("Cannot process terminal event");
        }
        status = EventStatus.PROCESSING;
        updatedAt = Instant.now();
    }

    /**
     * Complete the event with the classification result.
     * This method should only be called when the event is in PROCESSING status.
     * The status will be updated to COMPLETED after this method is called.
     * @param result
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
     * Register a failure for the event processing. This method should only be called when the event is in PROCESSING status.
     * The status will be updated to FAILED if the number of attempts exceeds the maxRetries.
     * The attempts count will be incremented by 1 after this method is called.
     * The status will remain PROCESSING if the number of attempts does not exceed the maxRetries.
     * The updatedAt timestamp will be updated to the current time after this method is called.
     * @param maxRetries
     * @return
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
