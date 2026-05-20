package infra.ai.bedrock;

import com.fasterxml.jackson.databind.ObjectMapper;
import domain.ClassificationResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BedrockClassificationResponseParserTest {

    private final BedrockClassificationResponseParser parser =
            new BedrockClassificationResponseParser(new ObjectMapper());

    @Test
    void shouldParseValidClassificationResponse() {
        String rawResponse = """
                {
                  "category": "PAYMENTS",
                  "subcategory": "TRANSFER",
                  "confidence": 0.95
                }
                """;

        ClassificationResult result = parser.parse(
                rawResponse,
                "amazon.nova-micro-v1:0",
                "banking-classifier-v1"
        );

        assertThat(result.category()).isEqualTo("PAYMENTS");
        assertThat(result.subcategory()).isEqualTo("TRANSFER");
        assertThat(result.confidence()).isEqualTo(0.95);
        assertThat(result.modelUsed()).isEqualTo("amazon.nova-micro-v1:0");
        assertThat(result.promptVersion()).isEqualTo("banking-classifier-v1");
    }

    @Test
    void shouldParseJsonEmbeddedInText() {
        String rawResponse = """
                Here is the classification:
                {
                  "category": "PAYMENTS",
                  "subcategory": "DIRECT_DEBIT",
                  "confidence": 0.91
                }
                """;

        ClassificationResult result = parser.parse(
                rawResponse,
                "amazon.nova-micro-v1:0",
                "banking-classifier-v1"
        );

        assertThat(result.category()).isEqualTo("PAYMENTS");
        assertThat(result.subcategory()).isEqualTo("DIRECT_DEBIT");
        assertThat(result.confidence()).isEqualTo(0.91);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("invalidResponses")
    void shouldRejectInvalidClassificationResponses(String testCase, String rawResponse) {
        assertThatThrownBy(() -> parser.parse(
                rawResponse,
                "amazon.nova-micro-v1:0",
                "banking-classifier-v1"
        )).isInstanceOf(IllegalArgumentException.class);
    }

    private static Stream<Arguments> invalidResponses() {
        return Stream.of(
                Arguments.of(
                        "blank response",
                        ""
                ),
                Arguments.of(
                        "missing json object",
                        "Here is the classification but no JSON"
                ),
                Arguments.of(
                        "missing category",
                        """
                        {
                          "subcategory": "TRANSFER",
                          "confidence": 0.95
                        }
                        """
                ),
                Arguments.of(
                        "missing subcategory",
                        """
                        {
                          "category": "PAYMENTS",
                          "confidence": 0.95
                        }
                        """
                ),
                Arguments.of(
                        "confidence above valid range",
                        """
                        {
                          "category": "PAYMENTS",
                          "subcategory": "TRANSFER",
                          "confidence": 1.50
                        }
                        """
                ),
                Arguments.of(
                        "confidence below valid range",
                        """
                        {
                          "category": "PAYMENTS",
                          "subcategory": "TRANSFER",
                          "confidence": -0.10
                        }
                        """
                ),
                Arguments.of(
                        "non numeric confidence",
                        """
                        {
                          "category": "PAYMENTS",
                          "subcategory": "TRANSFER",
                          "confidence": "HIGH"
                        }
                        """
                )
        );
    }
}