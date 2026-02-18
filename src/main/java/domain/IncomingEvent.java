package domain;

import java.time.Instant;

public record IncomingEvent(
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
        ClassificationResult classification
) {
}
