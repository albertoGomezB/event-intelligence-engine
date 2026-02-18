package application.ports;

import domain.ClassificationResult;

public interface EventClassifier {

    ClassificationResult classify(String payloadJson);
}
