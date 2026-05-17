package infra.dynamodb;

import application.ports.EventStore;
import domain.ClassificationResult;
import domain.EventStatus;
import domain.IncomingEvent;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class DynamoDbEventStore implements EventStore {

    public static final String EVENT_ID = "eventId";
    private final DynamoDbClient dynamoDbClient;
    private final String tableName;

    public DynamoDbEventStore(DynamoDbClient dynamoDbClient, String tableName) {
        this.dynamoDbClient = dynamoDbClient;
        this.tableName = tableName;
    }

    @Override
    public void save(IncomingEvent event) {
        dynamoDbClient.putItem(PutItemRequest.builder()
                .tableName(tableName)
                .item(toItem(event))
                .build());
    }

    @Override
    public Optional<IncomingEvent> findById(String eventId) {
        Map<String, AttributeValue> key = Map.of(
                EVENT_ID, AttributeValue.builder().s(eventId).build()
        );

        Map<String, AttributeValue> item = dynamoDbClient.getItem(GetItemRequest.builder()
                .tableName(tableName)
                .key(key)
                .build()).item();

        if (item == null || item.isEmpty()) {
            return Optional.empty();
        }

        return Optional.of(fromItem(item));
    }

    @Override
    public void update(IncomingEvent event) {
        save(event);
    }

    private Map<String, AttributeValue> toItem(IncomingEvent event) {
        Map<String, AttributeValue> item = new HashMap<>();
        item.put(EVENT_ID, AttributeValue.builder().s(event.getEventId()).build());
        item.put("correlationId", AttributeValue.builder().s(nullToEmpty(event.getCorrelationId())).build());
        item.put("source", AttributeValue.builder().s(nullToEmpty(event.getSource())).build());
        item.put("producer", AttributeValue.builder().s(nullToEmpty(event.getProducer())).build());
        item.put("originalType", AttributeValue.builder().s(nullToEmpty(event.getOriginalType())).build());
        item.put("payloadJson", AttributeValue.builder().s(event.getPayloadJson()).build());
        item.put("status", AttributeValue.builder().s(event.getStatus().name()).build());
        item.put("attempts", AttributeValue.builder().n(String.valueOf(event.getAttempts())).build());
        item.put("createdAt", AttributeValue.builder().s(event.getCreatedAt().toString()).build());
        item.put("updatedAt", AttributeValue.builder().s(event.getUpdatedAt().toString()).build());

        if (event.getClassification() != null) {
            item.put("classificationCategory", AttributeValue.builder().s(nullToEmpty(event.getClassification().category())).build());
            item.put("classificationSubcategory", AttributeValue.builder().s(nullToEmpty(event.getClassification().subcategory())).build());
            item.put("classificationConfidence", AttributeValue.builder().n(String.valueOf(event.getClassification().confidence())).build());
            item.put("classificationModelUsed", AttributeValue.builder().s(nullToEmpty(event.getClassification().modelUsed())).build());
            item.put("classificationPromptVersion", AttributeValue.builder().s(nullToEmpty(event.getClassification().promptVersion())).build());
        }

        return item;
    }

    private IncomingEvent fromItem(Map<String, AttributeValue> item) {
        String eventId = item.get(EVENT_ID).s();
        String correlationId = getString(item, "correlationId");
        String source = getString(item, "source");
        String producer = getString(item, "producer");
        String originalType = getString(item, "originalType");
        String payloadJson = item.get("payloadJson").s();

        IncomingEvent event = IncomingEvent.createNewEvent(eventId, correlationId, source, producer, originalType, payloadJson);

        // Best-effort state restoration with current domain API constraints.
        EventStatus targetStatus = EventStatus.valueOf(item.get("status").s());
        if (targetStatus == EventStatus.PROCESSING || targetStatus == EventStatus.COMPLETED || targetStatus == EventStatus.REVIEW_REQUIRED || targetStatus == EventStatus.FAILED) {
            event.startProcessing();
        }

        if (targetStatus == EventStatus.COMPLETED || targetStatus == EventStatus.REVIEW_REQUIRED) {
            ClassificationResult result = new ClassificationResult(
                    getString(item, "classificationCategory"),
                    getString(item, "classificationSubcategory"),
                    getDouble(item, "classificationConfidence"),
                    getString(item, "classificationModelUsed"),
                    getString(item, "classificationPromptVersion")
            );
            if (targetStatus == EventStatus.COMPLETED) {
                event.complete(result);
            } else {
                event.markForReview(result);
            }
        }

        if (targetStatus == EventStatus.FAILED) {
            event.registerFailure(1);
        }

        return event;
    }

    private static String getString(Map<String, AttributeValue> item, String key) {
        AttributeValue v = item.get(key);
        return v == null ? "" : v.s();
    }

    private static double getDouble(Map<String, AttributeValue> item, String key) {
        AttributeValue v = item.get(key);
        return v == null ? 0.0 : Double.parseDouble(v.n());
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
