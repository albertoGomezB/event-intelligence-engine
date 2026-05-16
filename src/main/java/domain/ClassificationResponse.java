package domain;

public record ClassificationResponse(boolean success,
                                     boolean temporaryFailure,
                                     boolean permanentFailure,
                                     ClassificationResult result,
                                     String errorMessage) {
}
