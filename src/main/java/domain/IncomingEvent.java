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
    private String reviewReason;
    private int attempts;
    private Instant createdAt;
    private Instant updatedAt;
    private ClassificationResult classification;

    private IncomingEvent(String eventId, String correlationId, String source, String producer,
                          String originalType, String payloadJson, EventStatus status, String reviewReason,
                          int attempts, Instant createdAt, Instant updatedAt,
                          ClassificationResult classification) {
        this.eventId = eventId;
        this.correlationId = correlationId;
        this.source = source;
        this.producer = producer;
        this.originalType = originalType;
        this.payloadJson = payloadJson;
        this.status = status;
        this.reviewReason = reviewReason;
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
                null,
                0,
                now,
                now,
                null
        );
    }

    /**
     * Rehydrates an event that was previously persisted by an infrastructure adapter.
     *
     * <p>This factory is intentionally separate from {@link #createNewEvent(String, String, String, String, String, String)}.
     * Creating a new event initializes lifecycle fields such as status, attempts and timestamps. Restoring an event must
     * preserve the durable state already stored in the persistence layer, including the current status, retry count,
     * timestamps and classification result.</p>
     *
     * <p>No lifecycle transition is executed here. The method reconstructs the aggregate exactly as it was persisted,
     * while still enforcing basic domain invariants to avoid silently loading corrupt state.</p>
     *
     * @param eventId unique event identifier
     * @param correlationId correlation identifier used for tracing the business flow
     * @param source source system or channel that originated the event
     * @param producer upstream producer that emitted the event
     * @param originalType original event type received from the producer
     * @param payloadJson original event payload serialized as JSON
     * @param status persisted lifecycle status
     * @param attempts number of processing attempts already registered
     * @param createdAt original event creation timestamp
     * @param updatedAt timestamp of the latest lifecycle update
     * @param classification persisted classification result, required for classified terminal states
     * @return restored incoming event with its persisted lifecycle state
     */
    public static IncomingEvent restore(
            String eventId,
            String correlationId,
            String source,
            String producer,
            String originalType,
            String payloadJson,
            EventStatus status,
            String reviewReason,
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
        if ((status == EventStatus.COMPLETED || status == EventStatus.HUMAN_REVIEW_REQUIRED) && classification == null) {
            throw new IllegalArgumentException("Classification result is required for classified terminal events");
        }
        if (status == EventStatus.HUMAN_REVIEW_REQUIRED &&
                (reviewReason == null || reviewReason.isBlank())) {
            throw new IllegalArgumentException(
                    "Review reason is required for review events");
        }

        return new IncomingEvent(
                eventId,
                correlationId,
                source,
                producer,
                originalType,
                payloadJson,
                status,
                reviewReason,
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
                this.status == EventStatus.HUMAN_REVIEW_REQUIRED) {
            throw new IllegalStateException("Cannot process terminal event");
        }
        this.attempts++;
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
    public void markForReview(ClassificationResult result, String reviewReason) {
        if(result == null) {
            throw new IllegalArgumentException("The result cannot be null");
        }
        if (this.status != EventStatus.PROCESSING) {
            throw new IllegalStateException("The status should be PROCESSING");
        }
        this.classification = result;
        this.reviewReason = reviewReason;
        updatedAt = Instant.now();
        this.status = EventStatus.HUMAN_REVIEW_REQUIRED;
    }

    /**
     * Registers a processing failure for an event currently in PROCESSING state.
     *
     * <p>Processing attempts are incremented when processing starts via
     * {@link #startProcessing()}, not when the failure is registered.</p>
     *
     * <p>If the number of processing attempts reaches the configured retry limit,
     * the event transitions to FAILED status. Otherwise, the event remains eligible
     * for retry processing.</p>
     *
     * <p>The updatedAt timestamp is refreshed when the failure is registered.</p>
     *
     * @param maxRetries maximum number of processing attempts allowed
     *                   before marking the event as FAILED
     *
     * @return true if the event can still be retried, false if the event
     *         reached the retry limit and was marked as FAILED
     */
    public boolean registerFailure (int maxRetries) {

        if (this.status != EventStatus.PROCESSING) {
            throw new IllegalStateException("Cannot register failure if event is not PROCESSING");
        }
        this.updatedAt = Instant.now();

        if (this.attempts >= maxRetries) {
            this.status = EventStatus.FAILED;
            return false;
        }
        return true;
    }

}
