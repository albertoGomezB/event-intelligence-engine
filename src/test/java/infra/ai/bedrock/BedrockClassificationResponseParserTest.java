package infra.ai.bedrock;

import com.fasterxml.jackson.databind.ObjectMapper;
import domain.ClassificationResult;
import org.junit.jupiter.api.Test;

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

    @Test
    void shouldRejectBlankResponse() {
        assertThatThrownBy(() -> parser.parse(
                "",
                "amazon.nova-micro-v1:0",
                "banking-classifier-v1"
        )).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldRejectMissingCategory() {
        String rawResponse = """
                {
                  "subcategory": "TRANSFER",
                  "confidence": 0.95
                }
                """;

        assertThatThrownBy(() -> parser.parse(
                rawResponse,
                "amazon.nova-micro-v1:0",
                "banking-classifier-v1"
        )).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldRejectMissingSubcategory() {
        String rawResponse = """
                {
                  "category": "PAYMENTS",
                  "confidence": 0.95
                }
                """;

        assertThatThrownBy(() -> parser.parse(
                rawResponse,
                "amazon.nova-micro-v1:0",
                "banking-classifier-v1"
        )).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldRejectConfidenceOutsideRange() {
        String rawResponse = """
                {
                  "category": "PAYMENTS",
                  "subcategory": "TRANSFER",
                  "confidence": 1.50
                }
                """;

        assertThatThrownBy(() -> parser.parse(
                rawResponse,
                "amazon.nova-micro-v1:0",
                "banking-classifier-v1"
        )).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldRejectNonNumericConfidence() {
        String rawResponse = """
                {
                  "category": "PAYMENTS",
                  "subcategory": "TRANSFER",
                  "confidence": "HIGH"
                }
                """;

        assertThatThrownBy(() -> parser.parse(
                rawResponse,
                "amazon.nova-micro-v1:0",
                "banking-classifier-v1"
        )).isInstanceOf(IllegalArgumentException.class);
    }
}