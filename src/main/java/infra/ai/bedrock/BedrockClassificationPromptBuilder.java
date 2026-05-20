package infra.ai.bedrock;

import domain.ClassificationRequest;

public class BedrockClassificationPromptBuilder {

    public String build(ClassificationRequest request) {
        return """
                You are a strict banking event classifier.

                Your task is to classify a banking domain event into exactly one category and one subcategory from the allowed taxonomy below.

                You must return valid JSON.
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

                UNCLASSIFIED:
                - AMBIGUOUS_EVENT
                - INSUFFICIENT_EVIDENCE
                - OUT_OF_SCOPE

                Classification rules:
                - Choose a business category only when the event contains enough evidence.
                - Choose the most specific subcategory only when it is clearly supported by the event.
                - If the category is clear but the subcategory is not clear, use the corresponding OTHER_* subcategory.
                - If the event could belong to multiple categories, return UNCLASSIFIED with subcategory AMBIGUOUS_EVENT.
                - If the event does not contain enough information to classify safely, return UNCLASSIFIED with subcategory INSUFFICIENT_EVIDENCE.
                - If the event is outside the supported banking taxonomy, return UNCLASSIFIED with subcategory OUT_OF_SCOPE.
                - Never force a business classification when the evidence is weak.
                - Never create a new category.
                - Never create a new subcategory.
                - The confidence value must be between 0.0 and 1.0.

                Confidence rules:
                - Do not use 0.95 as a default confidence value.
                - Use 0.90 to 1.00 only when source, producer, originalType and payload clearly support the same classification.
                - Use 0.70 to 0.89 when the classification is likely, but some details are missing.
                - Use 0.50 to 0.69 when the event is ambiguous or has limited evidence.
                - Use below 0.50 when the event has weak evidence or is outside the supported taxonomy.
                - UNCLASSIFIED results should usually have confidence below 0.70.

                Response schema:
                {
                  "category": "PAYMENTS | CARDS | FRAUD | LENDING | CUSTOMER_SUPPORT | UNCLASSIFIED",
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