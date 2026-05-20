package application.ports;

import domain.ClassificationRequest;
import domain.ClassificationResponse;

public interface EventClassifier {

    ClassificationResponse classify(ClassificationRequest request);
}
