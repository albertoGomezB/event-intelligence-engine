package domain;

/**
 * Input contract used by classifier adapters.
 * Contains only the fields required for classification to decouple from the full event structure.
 */
public record ClassificationRequest(
        String source,
        String producer,
        String originalType,
        String payloadJson
) {
    public ClassificationRequest {
        if (payloadJson == null || payloadJson.isBlank()) {
            throw new IllegalArgumentException("Payload JSON cannot be null or blank");
        }
    }
}