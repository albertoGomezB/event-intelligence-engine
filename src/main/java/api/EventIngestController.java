package api;

import application.IngestEventService;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/events")
public class EventIngestController {

    private final IngestEventService ingestEventService;

    public EventIngestController(IngestEventService ingestEventService) {
        this.ingestEventService = ingestEventService;
    }

    @PostMapping
    public ResponseEntity<IngestResponse> ingestEvent (@Validated @RequestBody IngestRequest request) {

        String eventId = ingestEventService.ingestEvent(
                request.correlationId(),
                request.source(),
                request.producer(),
                request.originalType(),
                request.payloadJson()
        );

        return ResponseEntity.accepted()
                .body(new IngestResponse(eventId));
    }
}
