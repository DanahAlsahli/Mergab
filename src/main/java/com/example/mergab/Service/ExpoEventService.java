package com.example.mergab.Service;



import com.example.mergab.Api.ApiException;
import com.example.mergab.Model.ExpoEvent;
import com.example.mergab.Model.Zone;
import com.example.mergab.Repository.ExpoEventRepository;
import com.example.mergab.Repository.ZoneRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ExpoEventService {


    private final ExpoEventRepository expoEventRepository;
    private final ZoneRepository zoneRepository;

    public List<ExpoEvent> getExpoEvents() {
        return expoEventRepository.findAll();
    }

    public void addExpoEvent(Integer zoneId, ExpoEvent expoEvent) {

        // Check if zone exists
        Zone zone = zoneRepository.findZoneById(zoneId);

        if (zone == null) {
            throw new ApiException("Zone not found");
        }

        // Validate event dates
        if (!expoEvent.getEndDateTime().isAfter(expoEvent.getStartDateTime())) {
            throw new ApiException("End date must be after start date");
        }

        expoEvent.setZone(zone);
        expoEventRepository.save(expoEvent);
    }


    //update expo event
    public void updateExpoEvent(Integer id, ExpoEvent expoEvent) {

        // Check if event exists
        ExpoEvent oldEvent = expoEventRepository.findExpoEventById(id);

        if (oldEvent == null) {
            throw new ApiException("Event not found");
        }

        // Validate event dates
        if (!expoEvent.getEndDateTime().isAfter(expoEvent.getStartDateTime())) {
            throw new ApiException("End date must be after start date");
        }

        oldEvent.setName(expoEvent.getName());
        oldEvent.setStartDateTime(expoEvent.getStartDateTime());
        oldEvent.setEndDateTime(expoEvent.getEndDateTime());
        oldEvent.setExpectedDemand(expoEvent.getExpectedDemand());

        expoEventRepository.save(oldEvent);
    }


    //delet expo event
    public void deleteExpoEvent(Integer id) {
        // Check if event exists
        ExpoEvent expoEvent = expoEventRepository.findExpoEventById(id);

        if (expoEvent == null) {
            throw new ApiException("Event not found");
        }

        expoEventRepository.delete(expoEvent);
    }


    //change event zone
    public void changeEventZone(Integer eventId, Integer zoneId) {

        // Check if event exists
        ExpoEvent expoEvent = expoEventRepository.findExpoEventById(eventId);

        if (expoEvent == null) {
            throw new ApiException("Event not found");
        }

        // Check if zone exists
        Zone zone = zoneRepository.findZoneById(zoneId);

        if (zone == null) {
            throw new ApiException("Zone not found");
        }

        // Prevent assigning the same zone
        if (expoEvent.getZone().getId().equals(zoneId)) {
            throw new ApiException("Event is already assigned to this zone");
        }

        expoEvent.setZone(zone);
        expoEventRepository.save(expoEvent);
    }

    // Get events by zone
    public List<ExpoEvent> getEventsByZone(Integer zoneId) {

        // Check if zone exists
        Zone zone = zoneRepository.findZoneById(zoneId);

        if (zone == null) {
            throw new ApiException("Zone not found");
        }

        return expoEventRepository.findByZone_Id(zoneId);
    }

    // Get event by ID
    public ExpoEvent getEventById(Integer id) {

        ExpoEvent expoEvent = expoEventRepository.findExpoEventById(id);

        // Check if event exists
        if (expoEvent == null) {
            throw new ApiException("Event not found");
        }

        return expoEvent;
    }
}
