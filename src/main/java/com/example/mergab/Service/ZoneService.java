package com.example.mergab.Service;


import com.example.mergab.Api.ApiException;
import com.example.mergab.Model.Zone;
import com.example.mergab.Repository.ZoneConnectionRepository;
import com.example.mergab.Repository.ZoneRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ZoneService {
    private final ZoneRepository zoneRepository;
    private final ZoneConnectionRepository zoneConnectionRepository;


    //get all zones
    public List<Zone>getZones(){
        return zoneRepository.findAll();
    }

    //add zone
    public void addZone(Zone zone) {

        Zone existingZone = zoneRepository.findZoneByName(zone.getName());

        if (existingZone != null) {
            throw new ApiException("Zone name already exists");
        }

        if (zone.getInitialVisitors() > zone.getCapacity()) {
            throw new ApiException("Initial visitors cannot exceed zone capacity");
        }

        zoneRepository.save(zone);
    }

    //update zone data
    public void updateZone(Integer zoneId, Zone zone) {

        Zone oldZone = zoneRepository.findZoneById(zoneId);

        if (oldZone == null) {
            throw new ApiException("Zone not found");
        }

        Zone existingZone = zoneRepository.findZoneByName(zone.getName());

        if (existingZone != null && !existingZone.getId().equals(zoneId)) {
            throw new ApiException("Zone name already exists");
        }

        if (zone.getInitialVisitors() > zone.getCapacity()) {
            throw new ApiException("Initial visitors cannot exceed zone capacity");
        }

        oldZone.setName(zone.getName());
        oldZone.setCapacity(zone.getCapacity());
        oldZone.setInitialVisitors(zone.getInitialVisitors());
        zoneRepository.save(oldZone);
    }

    // Delete zone
    @Transactional
    public void deleteZone(Integer id) {

        Zone zone = zoneRepository.findZoneById(id);

        // Check if zone exists
        if (zone == null) {
            throw new ApiException("Zone not found");
        }

        // Prevent deletion if zone has connections
        if (zoneConnectionRepository.existsByFromZone_IdOrToZone_Id(id, id)) {
            throw new ApiException("Cannot delete a zone with existing connections");
        }

        zoneRepository.delete(zone);
    }

    //get zone by name
    public Zone getZoneByName(String name) {
        Zone zone = zoneRepository.findZoneByName(name);

        if (zone == null) {
            throw new ApiException("Zone not found");
        }


        return zone;
    }

    // Get zone by ID
    public Zone getZoneById(Integer id) {
        Zone zone = zoneRepository.findZoneById(id);

        // Check if zone exists
        if (zone == null) {
            throw new ApiException("Zone not found");
        }

        return zone;
    }

    // Get available zones based on initial visitors
    public List<Zone> getAvailableZones() {
        return zoneRepository.findAvailableZones();
    }

    // Get initial occupancy percentage
    public double getZoneOccupancy(Integer zoneId) {
        Zone zone = getZoneById(zoneId);

        return (zone.getInitialVisitors() * 100.0) / zone.getCapacity();
    }
}
