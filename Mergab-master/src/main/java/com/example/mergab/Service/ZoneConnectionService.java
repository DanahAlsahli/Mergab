package com.example.mergab.Service;

import com.example.mergab.Api.ApiException;
import com.example.mergab.Model.Zone;
import com.example.mergab.Model.ZoneConnection;
import com.example.mergab.Repository.ZoneConnectionRepository;
import com.example.mergab.Repository.ZoneRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ZoneConnectionService {

    private final ZoneConnectionRepository zoneConnectionRepository;
    private final ZoneRepository zoneRepository;

    //get all zone connection
    public List<ZoneConnection> getZoneConnections() {
        return zoneConnectionRepository.findAll();
    }

    // Add zone connection
    public void addZoneConnection(Integer fromZoneId, Integer toZoneId, ZoneConnection zoneConnection) {

        Zone fromZone = zoneRepository.findZoneById(fromZoneId);
        Zone toZone = zoneRepository.findZoneById(toZoneId);

        // Check if zones exist
        if (fromZone == null || toZone == null) {
            throw new ApiException("Zone not found");
        }

        // Prevent self-connection
        if (fromZoneId.equals(toZoneId)) {
            throw new ApiException("A zone cannot connect to itself");
        }

        // Prevent duplicate connection
        if (zoneConnectionRepository.existsByFromZone_IdAndToZone_Id(fromZoneId, toZoneId)) {
            throw new ApiException("Zone connection already exists");
        }

        zoneConnection.setFromZone(fromZone);
        zoneConnection.setToZone(toZone);

        zoneConnectionRepository.save(zoneConnection);
    }

    // Update connection capacity
    public void updateZoneConnection(Integer id, ZoneConnection zoneConnection) {

        ZoneConnection oldConnection = zoneConnectionRepository.findZoneConnectionById(id);

        // Check if connection exists
        if (oldConnection == null) {
            throw new ApiException("Zone connection not found");
        }

        oldConnection.setMaxVisitorsPerMinute(zoneConnection.getMaxVisitorsPerMinute());

        zoneConnectionRepository.save(oldConnection);
    }

    // Change connected zones
    public void changeConnectionZones(Integer id, Integer fromZoneId, Integer toZoneId) {

        ZoneConnection oldConnection = zoneConnectionRepository.findZoneConnectionById(id);

        // Check if connection exists
        if (oldConnection == null) {
            throw new ApiException("Zone connection not found");
        }

        Zone fromZone = zoneRepository.findZoneById(fromZoneId);
        Zone toZone = zoneRepository.findZoneById(toZoneId);

        // Check if zones exist
        if (fromZone == null || toZone == null) {
            throw new ApiException("Zone not found");
        }

        // Prevent self-connection
        if (fromZoneId.equals(toZoneId)) {
            throw new ApiException("A zone cannot connect to itself");
        }

        // Prevent duplicate connection
        ZoneConnection existingConnection = zoneConnectionRepository.findByFromZone_IdAndToZone_Id(fromZoneId, toZoneId);

        if (existingConnection != null && !existingConnection.getId().equals(id)) {
            throw new ApiException("Zone connection already exists");
        }

        oldConnection.setFromZone(fromZone);
        oldConnection.setToZone(toZone);

        zoneConnectionRepository.save(oldConnection);
    }

    // Delete zone connection
    public void deleteZoneConnection(Integer id) {
        ZoneConnection zoneConnection = zoneConnectionRepository.findZoneConnectionById(id);
        // Check if connection exists
        if (zoneConnection == null) {
            throw new ApiException("Zone connection not found");
        }

        zoneConnectionRepository.delete(zoneConnection);
    }


    // Get connection by ID
    public ZoneConnection getZoneConnectionById(Integer id) {

        ZoneConnection connection = zoneConnectionRepository.findZoneConnectionById(id);

        // Check if connection exists
        if (connection == null) {
            throw new ApiException("Zone connection not found");
        }

        return connection;
    }

    // Get outgoing connections from a zone
    public List<ZoneConnection> getOutgoingConnections(Integer zoneId) {

        // Check if zone exists
        if (zoneRepository.findZoneById(zoneId) == null) {
            throw new ApiException("Zone not found");
        }

        return zoneConnectionRepository.findByFromZone_Id(zoneId);
    }

    // Get all connections related to a zone
    public List<ZoneConnection> getConnectionsByZone(Integer zoneId) {

        // Check if zone exists
        if (zoneRepository.findZoneById(zoneId) == null) {
            throw new ApiException("Zone not found");
        }

        return zoneConnectionRepository.findByFromZone_IdOrToZone_Id(zoneId, zoneId);
    }
}
