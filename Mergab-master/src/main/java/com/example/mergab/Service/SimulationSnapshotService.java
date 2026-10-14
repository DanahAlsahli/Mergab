package com.example.mergab.Service;



import com.example.mergab.Api.ApiException;
import com.example.mergab.Model.SimulationSnapshot;
import com.example.mergab.Model.Simulation;
import com.example.mergab.Model.Zone;
import com.example.mergab.Repository.SimulationRepository;
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
    private final SimulationRepository simulationRepository;

    // Get all snapshots
    public List<SimulationSnapshot> getSimulationSnapshots() {
        return simulationSnapshotRepository.findAll();
    }


    // Add snapshot
    public void addSimulationSnapshot(Integer simulationId, Integer zoneId, SimulationSnapshot snapshot) {

        // Check if simulation exists
        Simulation simulation = simulationRepository.findSimulationById(simulationId);

        if (simulation == null) {
            throw new ApiException("Simulation not found");
        }

        // Check if zone exists
        Zone zone = zoneRepository.findZoneById(zoneId);

        if (zone == null) {
            throw new ApiException("Zone not found");
        }

        // Prevent duplicate snapshot
        if (simulationSnapshotRepository.existsBySimulation_IdAndZone_IdAndMinute(simulationId, zoneId, snapshot.getMinute())) {
            throw new ApiException("Snapshot already exists for this simulation, zone and minute");
        }

        snapshot.setSimulation(simulation);
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

        // Prevent duplicate snapshot when changing minute
        if (!oldSnapshot.getMinute().equals(snapshot.getMinute()) && simulationSnapshotRepository.existsBySimulation_IdAndZone_IdAndMinute(
                oldSnapshot.getSimulation().getId(), oldSnapshot.getZone().getId(), snapshot.getMinute())) {

            throw new ApiException("Snapshot already exists for this simulation, zone and minute");
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

        // Prevent duplicate snapshot in the new zone
        if (simulationSnapshotRepository.existsBySimulation_IdAndZone_IdAndMinute(snapshot.getSimulation().getId(), zoneId, snapshot.getMinute())) {

            throw new ApiException("Snapshot already exists for this simulation, zone and minute");
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


    // Get snapshots by simulation
    public List<SimulationSnapshot> getSnapshotsBySimulation(Integer simulationId) {

        // Check if simulation exists
        if (!simulationRepository.existsById(simulationId)) {
            throw new ApiException("Simulation not found");
        }

        return simulationSnapshotRepository.findBySimulation_Id(simulationId);
    }

    // Get zone snapshots in a simulation
    public List<SimulationSnapshot> getSnapshotsBySimulationAndZone(Integer simulationId, Integer zoneId) {

        // Check if simulation exists
        if (!simulationRepository.existsById(simulationId)) {
            throw new ApiException("Simulation not found");
        }

        // Check if zone exists
        if (!zoneRepository.existsById(zoneId)) {
            throw new ApiException("Zone not found");
        }

        return simulationSnapshotRepository.findBySimulation_IdAndZone_IdOrderByMinuteAsc(simulationId, zoneId);
    }

    // Get all zone snapshots at a specific minute
    public List<SimulationSnapshot> getSnapshotsAtMinute(
            Integer simulationId, Integer minute) {

        // Check if simulation exists
        if (!simulationRepository.existsById(simulationId)) {
            throw new ApiException("Simulation not found");
        }

        // Validate minute
        if (minute < 0) {
            throw new ApiException("Minute cannot be negative");
        }

        return simulationSnapshotRepository.findBySimulation_IdAndMinute(simulationId, minute);
    }


    // Get peak occupancy for a zone in a simulation
    public SimulationSnapshot getPeakOccupancy(
            Integer simulationId, Integer zoneId) {

        // Check if simulation exists
        if (!simulationRepository.existsById(simulationId)) {
            throw new ApiException("Simulation not found");
        }

        // Check if zone exists
        if (!zoneRepository.existsById(zoneId)) {
            throw new ApiException("Zone not found");
        }

        SimulationSnapshot snapshot = simulationSnapshotRepository.findFirstBySimulation_IdAndZone_IdOrderByOccupancyDesc(simulationId, zoneId);

        // Check if snapshot data exists
        if (snapshot == null) {
            throw new ApiException("No snapshots found for this zone in the simulation");
        }

        return snapshot;
    }
}
