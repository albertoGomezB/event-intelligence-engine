package api;

public record IngestRequest(
        String correlationId,
        String source,
        String producer,
        String originalType,
        String payloadJson
) {}
