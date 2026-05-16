package application.ports;

public interface ClassificationPolicy {

    boolean requiresHumanReview(double confidence);

    boolean isCategoryAllowed(String category);
}
