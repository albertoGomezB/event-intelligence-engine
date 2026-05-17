package infra.sqs;

import application.ports.QueuePublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;

public class SqsQueuePublisher implements QueuePublisher {

    private static final Logger log = LoggerFactory.getLogger(SqsQueuePublisher.class);

    private final SqsClient sqsClient;
    private final String queueUrl;

    public SqsQueuePublisher(SqsClient sqsClient, String queueUrl) {
        this.sqsClient = sqsClient;
        this.queueUrl = queueUrl;
    }

    @Override
    public void publishEventId(String eventId) {
        sqsClient.sendMessage(SendMessageRequest.builder()
                .queueUrl(queueUrl)
                .messageBody(eventId)
                .build());
        log.info("event_published_sqs eventId={} queueUrl={}", eventId, queueUrl);
    }
}
