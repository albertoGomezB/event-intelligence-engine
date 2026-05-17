package infra.sqs;

import application.worker.EventProcessingWorker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.DeleteMessageRequest;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageResponse;

import java.util.List;

public class SqsEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(SqsEventConsumer.class);

    private final SqsClient sqsClient;
    private final String queueUrl;
    private final EventProcessingWorker worker;

    public SqsEventConsumer(SqsClient sqsClient, String queueUrl, EventProcessingWorker worker) {
        this.sqsClient = sqsClient;
        this.queueUrl = queueUrl;
        this.worker = worker;
    }

    @Scheduled(fixedDelayString = "${app.aws.sqs.consumer.fixed-delay-ms:1000}")
    public void pollAndProcess() {
        ReceiveMessageResponse response = sqsClient.receiveMessage(
                ReceiveMessageRequest.builder()
                        .queueUrl(queueUrl)
                        .maxNumberOfMessages(10)
                        .waitTimeSeconds(10) // long polling
                        .build()
        );

        List<Message> messages = response.messages();
        if (messages == null || messages.isEmpty()) {
            return;
        }

        for (Message message : messages) {
            String eventId = message.body();
            try {
                worker.process(eventId);

                sqsClient.deleteMessage(DeleteMessageRequest.builder()
                        .queueUrl(queueUrl)
                        .receiptHandle(message.receiptHandle())
                        .build());

                log.info("event_consumed_sqs eventId={} messageId={}", eventId, message.messageId());
            } catch (Exception ex) {
                log.error("event_consume_failed eventId={} messageId={} error={}",
                        eventId, message.messageId(), ex.getMessage(), ex);
                // no delete => SQS retry / DLQ policy
            }
        }
    }
}