package infra.ai.bedrock;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import domain.ClassificationResult;

public class BedrockClassificationResponseParser {

    private final ObjectMapper objectMapper;

    public BedrockClassificationResponseParser(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public ClassificationResult parse(String rawResponse, String modelId, String promptVersion) {
        if (rawResponse == null || rawResponse.isBlank()) {
            throw new IllegalArgumentException("Bedrock response cannot be null or blank");
        }

        try {
            String json = extractJsonObject(rawResponse);
            JsonNode root = objectMapper.readTree(json);

            String category = requiredText(root, "category");
            String subcategory = requiredText(root, "subcategory");
            double confidence = requiredDouble(root, "confidence");

            if (confidence < 0.0 || confidence > 1.0) {
                throw new IllegalArgumentException("Confidence must be between 0.0 and 1.0");
            }

            return new ClassificationResult(
                    category,
                    subcategory,
                    confidence,
                    modelId,
                    promptVersion
            );
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid Bedrock classification response", e);
        }
    }

    private static String extractJsonObject(String rawResponse) {
        int start = rawResponse.indexOf('{');
        int end = rawResponse.lastIndexOf('}');

        if (start < 0 || end < 0 || end <= start) {
            throw new IllegalArgumentException("Bedrock response does not contain a JSON object");
        }

        return rawResponse.substring(start, end + 1);
    }

    private static String requiredText(JsonNode root, String fieldName) {
        JsonNode node = root.get(fieldName);

        if (node == null || !node.isTextual() || node.asText().isBlank()) {
            throw new IllegalArgumentException("Missing or invalid text field: " + fieldName);
        }

        return node.asText();
    }

    private static double requiredDouble(JsonNode root, String fieldName) {
        JsonNode node = root.get(fieldName);

        if (node == null || !node.isNumber()) {
            throw new IllegalArgumentException("Missing or invalid numeric field: " + fieldName);
        }

        return node.asDouble();
    }
}