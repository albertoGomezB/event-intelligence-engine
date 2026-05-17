package api;

import domain.ClassificationResult;
import domain.EventStatus;

import java.time.Instant;

public record EventResponse(
        String eventId,
        String correlationId,
        String source,
        String producer,
        String originalType,
        EventStatus status,
        int attempts,
        Instant createdAt,
        Instant updatedAt,
        ClassificationResult classification
) {
}
