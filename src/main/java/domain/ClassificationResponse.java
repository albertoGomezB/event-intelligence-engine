package domain;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class ClassificationResponse {

    private final boolean success;
    private final boolean temporaryFailure;
    private final boolean permanentFailure;
    private final ClassificationResult result;
    private final String errorMessage;
}
