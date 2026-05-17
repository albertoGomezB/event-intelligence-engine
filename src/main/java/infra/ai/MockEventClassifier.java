package infra.ai;

import application.ports.EventClassifier;
import domain.ClassificationResponse;
import domain.ClassificationResult;

public class MockEventClassifier implements EventClassifier
{
    public ClassificationResponse classify(String payloadJson) {
        return new ClassificationResponse(
                true,
                false,
                false,
                new ClassificationResult(
                        "PAYMENTS",
                        "TRANSFER",
                        0.92,
                        "mock-classifier",
                        "prompt-v0"
                ),
                null
        );
    }
}
