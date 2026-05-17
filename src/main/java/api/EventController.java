package api;

import application.service.IngestEventService;
import application.service.GetEventService;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/events")
public class EventController {

    private final IngestEventService ingestEventService;
    private final GetEventService getEventService;

    public EventController(IngestEventService ingestEventService, GetEventService getEventService) {
        this.ingestEventService = ingestEventService;
        this.getEventService = getEventService;
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

    @GetMapping("/{eventId}")
    public ResponseEntity<EventResponse> getEvent(@PathVariable String eventId) {
        return getEventService.findById(eventId)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
