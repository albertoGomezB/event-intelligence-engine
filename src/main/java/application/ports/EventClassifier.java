package application.ports;

import domain.ClassificationResponse;

public interface EventClassifier {

    ClassificationResponse classify(String payloadJson);
}
