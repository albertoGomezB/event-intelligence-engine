package api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record IngestRequest(
        String correlationId,

        @NotBlank
        String source,

        @NotBlank
        String producer,

        @NotBlank
        String originalType,

        @NotBlank
        @Size(max = 32_000)
        String payloadJson
) {}
