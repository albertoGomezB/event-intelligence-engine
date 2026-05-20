package infra.policy;

import application.ports.ClassificationPolicy;

import java.util.Map;
import java.util.Set;

public class BankingClassificationPolicy implements ClassificationPolicy {

    private static final double MIN_CONFIDENCE_FOR_AUTO_CLASSIFICATION = 0.80;

    private static final Map<String, Set<String>> ALLOWED_CLASSIFICATIONS = Map.of(
            "PAYMENTS", Set.of(
                    "TRANSFER",
                    "DIRECT_DEBIT",
                    "CARD_PAYMENT",
                    "CASH_WITHDRAWAL",
                    "DEPOSIT",
                    "OTHER_PAYMENT"
            ),
            "CARDS", Set.of(
                    "CARD_ACTIVATION",
                    "CARD_BLOCK",
                    "CARD_TRANSACTION",
                    "CARD_LIMIT_CHANGE",
                    "OTHER_CARD"
            ),
            "FRAUD", Set.of(
                    "SUSPICIOUS_ACTIVITY",
                    "CHARGEBACK",
                    "ACCOUNT_TAKEOVER",
                    "FRAUD_ALERT",
                    "OTHER_FRAUD"
            ),
            "LENDING", Set.of(
                    "LOAN_APPLICATION",
                    "MORTGAGE",
                    "CREDIT_LINE",
                    "REPAYMENT",
                    "OTHER_LENDING"
            ),
            "CUSTOMER_SUPPORT", Set.of(
                    "COMPLAINT",
                    "SERVICE_REQUEST",
                    "INFORMATION_REQUEST",
                    "OTHER_SUPPORT"
            )
    );

    @Override
    public boolean requiresHumanReview(double confidence) {
        return confidence < MIN_CONFIDENCE_FOR_AUTO_CLASSIFICATION;
    }

    @Override
    public boolean isClassificationAllowed(String category, String subcategory) {
        if (category == null || subcategory == null) {
            return false;
        }

        Set<String> allowedSubcategories = ALLOWED_CLASSIFICATIONS.get(category);
        return allowedSubcategories != null && allowedSubcategories.contains(subcategory);
    }
}