package application.ports;

public interface ClassificationPolicy {

    boolean requiresHumanReview(double confidence);

    boolean isClassificationAllowed(String category, String subcategory);
}
