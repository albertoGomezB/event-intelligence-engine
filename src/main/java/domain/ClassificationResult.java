package domain;

public record ClassificationResult(
        String category,
        String subcategory,
        double confidence,
        String modelUsed,
        String promptVersion
) {
}
