package com.example.mergab.Service;



import com.example.mergab.Api.ApiException;
import com.example.mergab.Model.SimulationSnapshot;
import com.example.mergab.Model.Zone;
import com.example.mergab.Repository.SimulationSnapshotRepository;
import com.example.mergab.Repository.ZoneRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SimulationSnapshotService {

    private final SimulationSnapshotRepository simulationSnapshotRepository;
    private final ZoneRepository zoneRepository;

    // Get all snapshots
    public List<SimulationSnapshot> getSimulationSnapshots() {
        return simulationSnapshotRepository.findAll();
    }


    // Add snapshot
    public void addSimulationSnapshot(Integer zoneId, SimulationSnapshot snapshot) {

        // Check if zone exists
        Zone zone = zoneRepository.findZoneById(zoneId);

        if (zone == null) {
            throw new ApiException("Zone not found");
        }

        snapshot.setZone(zone);
        simulationSnapshotRepository.save(snapshot);
    }

    // Update snapshot
    public void updateSimulationSnapshot(Integer id, SimulationSnapshot snapshot) {

        SimulationSnapshot oldSnapshot = simulationSnapshotRepository.findSimulationSnapshotById(id);

        // Check if snapshot exists
        if (oldSnapshot == null) {
            throw new ApiException("Simulation snapshot not found");
        }

        oldSnapshot.setMinute(snapshot.getMinute());
        oldSnapshot.setOccupancy(snapshot.getOccupancy());
        oldSnapshot.setQueueLength(snapshot.getQueueLength());

        simulationSnapshotRepository.save(oldSnapshot);
    }

    // Change snapshot zone
    public void changeSnapshotZone(Integer snapshotId, Integer zoneId) {

        SimulationSnapshot snapshot = simulationSnapshotRepository.findSimulationSnapshotById(snapshotId);

        // Check if snapshot exists
        if (snapshot == null) {
            throw new ApiException("Simulation snapshot not found");
        }

        // Check if zone exists
        Zone zone = zoneRepository.findZoneById(zoneId);

        if (zone == null) {
            throw new ApiException("Zone not found");
        }

        // Prevent assigning the same zone
        if (snapshot.getZone().getId().equals(zoneId)) {
            throw new ApiException("Snapshot is already assigned to this zone");
        }

        snapshot.setZone(zone);
        simulationSnapshotRepository.save(snapshot);
    }

    // Delete snapshot
    public void deleteSimulationSnapshot(Integer id) {

        SimulationSnapshot snapshot = simulationSnapshotRepository.findSimulationSnapshotById(id);

        // Check if snapshot exists
        if (snapshot == null) {
            throw new ApiException("Simulation snapshot not found");
        }

        simulationSnapshotRepository.delete(snapshot);
    }


    // Get zone snapshots ordered by minute
    public List<SimulationSnapshot> getSnapshotsByZoneOrdered(Integer zoneId) {

        // Check if zone exists
        Zone zone = zoneRepository.findZoneById(zoneId);

        if (zone == null) {
            throw new ApiException("Zone not found");
        }

        return simulationSnapshotRepository.findByZone_IdOrderByMinuteAsc(zoneId);
    }

    // Get snapshot by ID
    public SimulationSnapshot getSnapshotById(Integer id) {

        SimulationSnapshot snapshot = simulationSnapshotRepository.findSimulationSnapshotById(id);

        // Check if snapshot exists
        if (snapshot == null) {
            throw new ApiException("Simulation snapshot not found");
        }

        return snapshot;
    }

    // Get snapshots by zone
    public List<SimulationSnapshot> getSnapshotsByZone(Integer zoneId) {

        // Check if zone exists
        Zone zone = zoneRepository.findZoneById(zoneId);

        if (zone == null) {
            throw new ApiException("Zone not found");
        }

        return simulationSnapshotRepository.findByZone_Id(zoneId);
    }
}
