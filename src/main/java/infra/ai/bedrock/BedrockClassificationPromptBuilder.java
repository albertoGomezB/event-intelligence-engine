package infra.ai.bedrock;

import domain.ClassificationRequest;

public class BedrockClassificationPromptBuilder {

    public String build(ClassificationRequest request) {
        return """
                You are a deterministic banking event classifier.

                Your task is to classify a banking domain event into exactly one category and one subcategory from the allowed taxonomy below.

                You must return only valid JSON.
                Do not include markdown.
                Do not include explanations.
                Do not include comments.
                Do not include fields outside the response schema.
                Do not invent categories or subcategories.

                Allowed taxonomy:

                PAYMENTS:
                - TRANSFER
                - DIRECT_DEBIT
                - CARD_PAYMENT
                - CASH_WITHDRAWAL
                - DEPOSIT
                - OTHER_PAYMENT

                CARDS:
                - CARD_ACTIVATION
                - CARD_BLOCK
                - CARD_TRANSACTION
                - CARD_LIMIT_CHANGE
                - OTHER_CARD

                FRAUD:
                - SUSPICIOUS_ACTIVITY
                - CHARGEBACK
                - ACCOUNT_TAKEOVER
                - FRAUD_ALERT
                - OTHER_FRAUD

                LENDING:
                - LOAN_APPLICATION
                - MORTGAGE
                - CREDIT_LINE
                - REPAYMENT
                - OTHER_LENDING

                CUSTOMER_SUPPORT:
                - COMPLAINT
                - SERVICE_REQUEST
                - INFORMATION_REQUEST
                - OTHER_SUPPORT

                Classification rules:
                - Choose the most specific subcategory allowed by the taxonomy.
                - If the event is clear, use a specific subcategory.
                - If the category is clear but the subcategory is ambiguous, use the corresponding OTHER_* subcategory.
                - If the whole event is ambiguous, choose the closest category and lower the confidence.
                - Never create a new category.
                - Never create a new subcategory.
                - The confidence value must be between 0.0 and 1.0.

                Confidence scale:
                - 0.90 to 1.00: very clear classification with strong evidence.
                - 0.70 to 0.89: likely classification, but some details are missing.
                - 0.50 to 0.69: ambiguous event with limited evidence.
                - Below 0.50: weak evidence or highly uncertain classification.

                Response schema:
                {
                  "category": "PAYMENTS | CARDS | FRAUD | LENDING | CUSTOMER_SUPPORT",
                  "subcategory": "one allowed subcategory from the selected category",
                  "confidence": 0.0
                }

                Event:
                {
                  "source": "%s",
                  "producer": "%s",
                  "originalType": "%s",
                  "payloadJson": %s
                }
                """.formatted(
                request.source(),
                request.producer(),
                request.originalType(),
                request.payloadJson()
        );
    }
}