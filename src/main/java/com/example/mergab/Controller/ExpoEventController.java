package com.example.mergab.Controller;

import com.example.mergab.Api.ApiResponse;
import com.example.mergab.Model.ExpoEvent;
import com.example.mergab.Service.ExpoEventService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

// TODO (Security): بدّلي {organizerId} بـ @AuthenticationPrincipal User user
@RestController
@RequestMapping("/api/v1/expo-event")
@RequiredArgsConstructor
public class ExpoEventController {

    private final ExpoEventService expoEventService;

    // Get all events
    @GetMapping("/get")
    public ResponseEntity<?> getExpoEvents() {
        return ResponseEntity.status(200).body(expoEventService.getExpoEvents());
    }

    // Get organizer's own events
    @GetMapping("/my-events/{organizerId}")
    public ResponseEntity<?> getMyEvents(@PathVariable Integer organizerId) {
        return ResponseEntity.status(200).body(expoEventService.getMyEvents(organizerId));
    }

    // Add event
    @PostMapping("/add/{organizerId}/{zoneId}")
    public ResponseEntity<?> addExpoEvent(@PathVariable Integer organizerId, @PathVariable Integer zoneId, @RequestBody @Valid ExpoEvent expoEvent) {
        expoEventService.addExpoEvent(organizerId, zoneId, expoEvent);
        return ResponseEntity.status(201).body(new ApiResponse("Event added successfully"));
    }

    // Update event
    @PutMapping("/update/{organizerId}/{id}")
    public ResponseEntity<?> updateExpoEvent(@PathVariable Integer organizerId, @PathVariable Integer id, @RequestBody @Valid ExpoEvent expoEvent) {
        expoEventService.updateExpoEvent(organizerId, id, expoEvent);
        return ResponseEntity.status(200).body(new ApiResponse("Event updated successfully"));
    }

    // Change event zone
    @PutMapping("/change-zone/{organizerId}/{eventId}/{zoneId}")
    public ResponseEntity<?> changeEventZone(@PathVariable Integer organizerId, @PathVariable Integer eventId, @PathVariable Integer zoneId) {
        expoEventService.changeEventZone(organizerId, eventId, zoneId);
        return ResponseEntity.status(200).body(new ApiResponse("Event zone changed successfully"));
    }

    // Delete event
    @DeleteMapping("/delete/{organizerId}/{id}")
    public ResponseEntity<?> deleteExpoEvent(@PathVariable Integer organizerId, @PathVariable Integer id) {
        expoEventService.deleteExpoEvent(organizerId, id);
        return ResponseEntity.status(200).body(new ApiResponse("Event deleted successfully"));
    }

    // Get event by ID
    @GetMapping("/get-by-id/{id}")
    public ResponseEntity<?> getEventById(@PathVariable Integer id) {
        return ResponseEntity.status(200).body(expoEventService.getEventById(id));
    }

    // Get events by zone
    @GetMapping("/get-by-zone/{zoneId}")
    public ResponseEntity<?> getEventsByZone(@PathVariable Integer zoneId) {
        return ResponseEntity.status(200).body(expoEventService.getEventsByZone(zoneId));
    }
}
