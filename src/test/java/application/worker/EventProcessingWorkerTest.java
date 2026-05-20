package application.worker;

import application.ports.ClassificationPolicy;
import application.ports.EventClassifier;
import application.ports.EventStore;
import application.ports.QueuePublisher;
import domain.ClassificationResponse;
import domain.ClassificationResult;
import domain.EventStatus;
import domain.IncomingEvent;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class EventProcessingWorkerTest {

    @Test
    void shouldCompleteEventWhenClassificationIsSuccessfulAndAllowed() {
        InMemoryEventStore eventStore = new InMemoryEventStore();
        CapturingQueuePublisher queuePublisher = new CapturingQueuePublisher();
        IncomingEvent event = newEvent("event-1");
        eventStore.save(event);

        EventProcessingWorker worker = new EventProcessingWorker(
                eventStore,
                classifierReturning(successfulResult("PAYMENTS", 0.92)),
                queuePublisher,
                allowingPolicy()
        );

        worker.process("event-1");

        IncomingEvent storedEvent = eventStore.findById("event-1").orElseThrow();
        assertThat(storedEvent.getStatus()).isEqualTo(EventStatus.COMPLETED);
        assertThat(storedEvent.getClassification().category()).isEqualTo("PAYMENTS");
        assertThat(storedEvent.getClassification().subcategory()).isEqualTo("DIRECT_DEBIT");
        assertThat(queuePublisher.publishedEventIds).isEmpty();
    }

    @Test
    void shouldMarkEventForReviewWhenConfidenceIsLow() {
        InMemoryEventStore eventStore = new InMemoryEventStore();
        IncomingEvent event = newEvent("event-1");
        eventStore.save(event);

        EventProcessingWorker worker = new EventProcessingWorker(
                eventStore,
                classifierReturning(successfulResult("PAYMENTS", 0.50)),
                new CapturingQueuePublisher(),
                new FixedClassificationPolicy(true, true)
        );

        worker.process("event-1");

        IncomingEvent storedEvent = eventStore.findById("event-1").orElseThrow();
        assertThat(storedEvent.getStatus()).isEqualTo(EventStatus.REVIEW_REQUIRED);
        assertThat(storedEvent.getClassification().confidence()).isEqualTo(0.50);
    }

    @Test
    void shouldMarkEventForReviewWhenCategoryIsNotAllowed() {
        InMemoryEventStore eventStore = new InMemoryEventStore();
        IncomingEvent event = newEvent("event-1");
        eventStore.save(event);

        EventProcessingWorker worker = new EventProcessingWorker(
                eventStore,
                classifierReturning(successfulResult("UNSUPPORTED", 0.95)),
                new CapturingQueuePublisher(),
                new FixedClassificationPolicy(false, false)
        );

        worker.process("event-1");

        IncomingEvent storedEvent = eventStore.findById("event-1").orElseThrow();
        assertThat(storedEvent.getStatus()).isEqualTo(EventStatus.REVIEW_REQUIRED);
        assertThat(storedEvent.getClassification().category()).isEqualTo("UNSUPPORTED");
    }

    @Test
    void shouldRepublishEventWhenClassifierFailsTemporarilyAndRetriesRemain() {
        InMemoryEventStore eventStore = new InMemoryEventStore();
        CapturingQueuePublisher queuePublisher = new CapturingQueuePublisher();
        IncomingEvent event = newEvent("event-1");
        eventStore.save(event);

        EventProcessingWorker worker = new EventProcessingWorker(
                eventStore,
                classifierReturning(new ClassificationResponse(false, true, false, null, "temporary error")),
                queuePublisher,
                allowingPolicy()
        );

        worker.process("event-1");

        IncomingEvent storedEvent = eventStore.findById("event-1").orElseThrow();
        assertThat(storedEvent.getStatus()).isEqualTo(EventStatus.PROCESSING);
        assertThat(storedEvent.getAttempts()).isEqualTo(1);
        assertThat(queuePublisher.publishedEventIds).containsExactly("event-1");
    }

    @Test
    void shouldFailEventWhenClassifierFailsPermanently() {
        InMemoryEventStore eventStore = new InMemoryEventStore();
        CapturingQueuePublisher queuePublisher = new CapturingQueuePublisher();
        IncomingEvent event = newEvent("event-1");
        eventStore.save(event);

        EventProcessingWorker worker = new EventProcessingWorker(
                eventStore,
                classifierReturning(new ClassificationResponse(false, false, true, null, "permanent error")),
                queuePublisher,
                allowingPolicy()
        );

        worker.process("event-1");

        IncomingEvent storedEvent = eventStore.findById("event-1").orElseThrow();
        assertThat(storedEvent.getStatus()).isEqualTo(EventStatus.FAILED);
        assertThat(storedEvent.getAttempts()).isEqualTo(1);
        assertThat(queuePublisher.publishedEventIds).isEmpty();
    }

    @Test
    void shouldIgnoreTerminalEvents() {
        InMemoryEventStore eventStore = new InMemoryEventStore();
        CapturingQueuePublisher queuePublisher = new CapturingQueuePublisher();
        IncomingEvent event = newEvent("event-1");
        event.startProcessing();
        event.complete(successfulResult("PAYMENTS", 0.92));
        eventStore.save(event);

        EventProcessingWorker worker = new EventProcessingWorker(
                eventStore,
                classifierReturning(successfulResult("CARDS", 0.99)),
                queuePublisher,
                allowingPolicy()
        );

        worker.process("event-1");

        IncomingEvent storedEvent = eventStore.findById("event-1").orElseThrow();
        assertThat(storedEvent.getStatus()).isEqualTo(EventStatus.COMPLETED);
        assertThat(storedEvent.getClassification().category()).isEqualTo("PAYMENTS");
        assertThat(queuePublisher.publishedEventIds).isEmpty();
    }

    private static IncomingEvent newEvent(String eventId) {
        return IncomingEvent.createNewEvent(
                eventId,
                "correlation-1",
                "mobile-banking",
                "payments-service",
                "TRANSFER_CREATED",
                "{\"amount\":100}"
        );
    }

    private static ClassificationResult successfulResult(String category, double confidence) {
        return new ClassificationResult(category, "DIRECT_DEBIT", confidence, "test-classifier", "prompt-test");
    }

    private static EventClassifier classifierReturning(ClassificationResult result) {
        return payloadJson -> new ClassificationResponse(true, false, false, result, null);
    }

    private static EventClassifier classifierReturning(ClassificationResponse response) {
        return payloadJson -> response;
    }

    private static ClassificationPolicy allowingPolicy() {
        return new FixedClassificationPolicy(true, false);
    }

    private static class FixedClassificationPolicy implements ClassificationPolicy {

        private final boolean classificationAllowed;
        private final boolean requiresHumanReview;

        private FixedClassificationPolicy(boolean classificationAllowed, boolean requiresHumanReview) {
            this.classificationAllowed = classificationAllowed;
            this.requiresHumanReview = requiresHumanReview;
        }

        @Override
        public boolean requiresHumanReview(double confidence) {
            return requiresHumanReview;
        }

        @Override
        public boolean isClassificationAllowed(String category, String subcategory) {
            return classificationAllowed;
        }
    }

    private static class CapturingQueuePublisher implements QueuePublisher {

        private final List<String> publishedEventIds = new ArrayList<>();

        @Override
        public void publishEventId(String eventId) {
            publishedEventIds.add(eventId);
        }
    }

    private static class InMemoryEventStore implements EventStore {

        private IncomingEvent event;

        @Override
        public void save(IncomingEvent event) {
            this.event = event;
        }

        @Override
        public Optional<IncomingEvent> findById(String eventId) {
            if (event == null || !event.getEventId().equals(eventId)) {
                return Optional.empty();
            }
            return Optional.of(event);
        }

        @Override
        public void update(IncomingEvent event) {
            save(event);
        }
    }
}
