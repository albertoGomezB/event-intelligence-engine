package infra.ai.bedrock;

import application.ports.EventClassifier;
import domain.ClassificationRequest;
import domain.ClassificationResponse;
import domain.ClassificationResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeClient;
import software.amazon.awssdk.services.bedrockruntime.model.BedrockRuntimeException;
import software.amazon.awssdk.services.bedrockruntime.model.ContentBlock;
import software.amazon.awssdk.services.bedrockruntime.model.ConversationRole;
import software.amazon.awssdk.services.bedrockruntime.model.ConverseRequest;
import software.amazon.awssdk.services.bedrockruntime.model.ConverseResponse;
import software.amazon.awssdk.services.bedrockruntime.model.InferenceConfiguration;
import software.amazon.awssdk.services.bedrockruntime.model.Message;

public class BedrockEventClassifier implements EventClassifier {

    private static final Logger log = LoggerFactory.getLogger(BedrockEventClassifier.class);

    private final BedrockRuntimeClient bedrockRuntimeClient;
    private final BedrockClassificationPromptBuilder promptBuilder;
    private final BedrockClassificationResponseParser responseParser;
    private final String modelId;
    private final String promptVersion;
    private final int maxTokens;
    private final float temperature;

    public BedrockEventClassifier(BedrockRuntimeClient bedrockRuntimeClient,
                                  BedrockClassificationPromptBuilder promptBuilder,
                                  BedrockClassificationResponseParser responseParser,
                                  String modelId,
                                  String promptVersion,
                                  int maxTokens,
                                  float temperature) {
        this.bedrockRuntimeClient = bedrockRuntimeClient;
        this.promptBuilder = promptBuilder;
        this.responseParser = responseParser;
        this.modelId = modelId;
        this.promptVersion = promptVersion;
        this.maxTokens = maxTokens;
        this.temperature = temperature;
    }

    @Override
    public ClassificationResponse classify(ClassificationRequest request) {
        String prompt = promptBuilder.build(request);

        try {
            ConverseResponse response = bedrockRuntimeClient.converse(
                    ConverseRequest.builder()
                            .modelId(modelId)
                            .messages(Message.builder()
                                    .role(ConversationRole.USER)
                                    .content(ContentBlock.fromText(prompt))
                                    .build())
                            .inferenceConfig(InferenceConfiguration.builder()
                                    .maxTokens(maxTokens)
                                    .temperature(temperature)
                                    .build())
                            .build()
            );

            String rawResponse = extractText(response);
            ClassificationResult result = responseParser.parse(rawResponse, modelId, promptVersion);

            log.info("bedrock_classification_success modelId={} promptVersion={}",
                    modelId, promptVersion);

            return new ClassificationResponse(
                    true,
                    false,
                    false,
                    result,
                    null
            );

        } catch (IllegalArgumentException e) {
            log.warn("bedrock_classification_invalid_response modelId={} promptVersion={} error={}",
                    modelId, promptVersion, e.getMessage());

            return new ClassificationResponse(
                    false,
                    false,
                    true,
                    null,
                    "Invalid Bedrock classification response: " + e.getMessage()
            );

        } catch (BedrockRuntimeException e) {
            boolean temporary = isTemporaryBedrockFailure(e);

            log.warn("bedrock_classification_failure modelId={} promptVersion={} statusCode={} temporary={} error={}",
                    modelId, promptVersion, e.statusCode(), temporary, e.getMessage());

            return new ClassificationResponse(
                    false,
                    temporary,
                    !temporary,
                    null,
                    e.getMessage()
            );

        } catch (SdkClientException e) {
            log.warn("bedrock_classification_client_failure modelId={} promptVersion={} error={}",
                    modelId, promptVersion, e.getMessage());

            return new ClassificationResponse(
                    false,
                    true,
                    false,
                    null,
                    e.getMessage()
            );
        }
    }

    private static String extractText(ConverseResponse response) {
        if (response == null ||
                response.output() == null ||
                response.output().message() == null ||
                response.output().message().content() == null ||
                response.output().message().content().isEmpty()) {
            throw new IllegalArgumentException("Bedrock response does not contain message content");
        }

        String text = response.output().message().content().getFirst().text();

        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException("Bedrock response text cannot be null or blank");
        }

        return text;
    }

    private static boolean isTemporaryBedrockFailure(BedrockRuntimeException e) {
        int statusCode = e.statusCode();
        return statusCode == 429 || statusCode >= 500;
    }
}