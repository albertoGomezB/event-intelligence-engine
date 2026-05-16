package infra.policy;

import application.ports.ClassificationPolicy;

import java.util.Set;

public class BankingClassificationPolicy implements ClassificationPolicy {

    private static final double MIN_CONFIDENCE_FOR_AUTO_CLASSIFICATION = 0.80;

    private static final Set<String> ALLOWED_CATEGORIES = Set.of(
            "PAYMENTS",
            "CARDS",
            "FRAUD",
            "LENDING",
            "CUSTOMER_SUPPORT"
    );

    @Override
    public boolean requiresHumanReview(double confidence) {
        return confidence < MIN_CONFIDENCE_FOR_AUTO_CLASSIFICATION;
    }

    @Override
    public boolean isCategoryAllowed(String category) {
        return category != null && ALLOWED_CATEGORIES.contains(category);
    }
}
