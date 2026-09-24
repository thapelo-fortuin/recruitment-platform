package com.recruitplatform.soc_service.controller;
import com.recruitplatform.soc_service.model.Event;
import com.recruitplatform.soc_service.repository.EventRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
@RestController @RequestMapping("/api/v1/events") public class EventController {
    private final EventRepository eventRepository;
    public EventController(EventRepository eventRepository) { this.eventRepository = eventRepository; }
    @PostMapping public ResponseEntity<Event> ingest(@RequestBody Event event) {
        if (event.getEventId() == null || event.getEventId().isBlank()) {
            event.setEventId("evt-" + UUID.randomUUID());
        }
        if (event.getTimestamp() == null) {
            event.setTimestamp(LocalDateTime.now());
        }
        Event saved = eventRepository.save(event);
        return ResponseEntity.ok(saved); }
    @GetMapping
    public List<Event> getAll(@RequestParam(required = false) String eventType) {
        if (eventType == null || eventType.isBlank()) {
            return eventRepository.findAllByOrderByTimestampDesc();
        }
        return eventRepository.findByEventTypeOrderByTimestampDesc(eventType);
    }
}