package com.example.mergab.Service;

import com.example.mergab.Api.ApiException;
import com.example.mergab.Model.HistoricalAttendance;
import com.example.mergab.Model.Organizer;
import com.example.mergab.Model.Simulation;
import com.example.mergab.Model.SimulationSnapshot;
import com.example.mergab.Model.Zone;
import com.example.mergab.Repository.SimulationSnapshotRepository;
import com.example.mergab.Repository.ZoneConnectionRepository;
import com.example.mergab.Repository.ZoneRepository;
import com.example.mergab.Simulation.*;
import com.example.mergab.Repository.HistoricalAttendanceRepository;
import com.example.mergab.Repository.OrganizerRepository;
import com.example.mergab.Repository.SimulationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.example.mergab.Model.ExpoEvent;
import com.example.mergab.Repository.ExpoEventRepository;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SimulationService {

    private final SimulationRepository simulationRepository;
    private final HistoricalAttendanceRepository historicalAttendanceRepository;
    private final OrganizerRepository organizerRepository;
    private final SimulationSnapshotRepository simulationSnapshotRepository;
    private final ZoneRepository zoneRepository;
    private final ZoneConnectionRepository zoneConnectionRepository;
    private final ExpoEventRepository expoEventRepository;

    private final CrowdSimulationEngine crowdSimulationEngine = new CrowdSimulationEngine();
    private final EventDrivenSimulationRunner eventDrivenSimulationRunner =
            new EventDrivenSimulationRunner(
                    crowdSimulationEngine,
                    new EventDrivenSimulationEngine()
            );

    // كل المحاكاة (للأدمن)
    public List<Simulation> getAllSimulations() {
        return simulationRepository.findAll();
    }

    // المحاكاة اللي أنشأها منظم معيّن
    public List<Simulation> getSimulationsByOrganizer(Integer organizerId) {
        getOrganizer(organizerId);
        return simulationRepository.findSimulationsByOrganizer_Id(organizerId);
    }

    public void addSimulation(Integer organizerId, Integer historicalAttendanceId, Simulation simulation) {
        Organizer organizer = getOrganizer(organizerId);

        HistoricalAttendance attendance = historicalAttendanceRepository.findHistoricalAttendanceById(historicalAttendanceId);
        if (attendance == null) {
            throw new ApiException("Historical attendance not found");
        }

        simulation.setHistoricalAttendance(attendance);
        simulation.setOrganizer(organizer);
        // هذي يحددها النظام، مو المستخدم
        simulation.setStatus("PENDING");
        simulation.setStartDateTime(null);
        simulation.setEndDateTime(null);

        simulationRepository.save(simulation);
    }

    // التعديل مسموح بس قبل التشغيل
    public void updateSimulation(Integer id, Simulation newSimulation) {
        Simulation oldSimulation = getSimulation(id);
        if (!oldSimulation.getStatus().equals("PENDING")) {
            throw new ApiException("Only pending simulations can be updated");
        }

        oldSimulation.setDurationMinutes(newSimulation.getDurationMinutes());
        oldSimulation.setRandomSeed(newSimulation.getRandomSeed());

        simulationRepository.save(oldSimulation);
    }

    /** Run a pending simulation and persist one snapshot per zone per minute. */
    @Transactional
    public List<SimulationSnapshot> runSimulation(Integer simulationId) {
        Simulation simulation = getSimulation(simulationId);
        if (!"PENDING".equals(simulation.getStatus())) {
            throw new ApiException("Only pending simulations can be run");
        }

        List<Zone> zones = zoneRepository.findAll();
        if (zones.isEmpty()) {
            throw new ApiException("Cannot run a simulation without zones");
        }

        for (Zone zone : zones) {
            if (zone.getInitialVisitors() > zone.getCapacity()) {
                throw new ApiException("Initial visitors exceed capacity for zone: " + zone.getName());
            }
        }

        simulation.setStatus("RUNNING");
        simulation.setStartDateTime(LocalDateTime.now());
        simulationRepository.save(simulation);

        LocalDateTime simulationStart = simulation.getStartDateTime();
        LocalDateTime simulationEnd = simulationStart.plusMinutes(
                simulation.getDurationMinutes()
        );

        List<ExpoEvent> events = expoEventRepository
                .findExpoEventsByOrganizer_Id(
                        simulation.getOrganizer().getId()
                )
                .stream()
                .filter(event ->
                        event.getStartDateTime().isBefore(simulationEnd)
                                && event.getEndDateTime().isAfter(simulationStart)
                )
                .toList();

        List<SimulationSnapshotRecord> records =
                eventDrivenSimulationRunner.runSimulation(
                        zones,
                        zoneConnectionRepository.findAll(),
                        events,
                        simulationStart,
                        simulation.getDurationMinutes(),
                        30
                );

        Map<Integer, Zone> zonesById = zones.stream().collect(
                Collectors.toMap(Zone::getId, Function.identity())
        );
        List<SimulationSnapshot> snapshots = records.stream().map(record -> {
            Zone zone = zonesById.get(record.zoneId());
            if (zone == null) {
                throw new ApiException("Simulation produced a snapshot for an unknown zone");
            }
            SimulationSnapshot snapshot = new SimulationSnapshot();
            snapshot.setSimulation(simulation);
            snapshot.setZone(zone);
            snapshot.setMinute(record.minute());
            snapshot.setOccupancy(record.occupancy());
            snapshot.setQueueLength(record.queueLength());
            return snapshot;
        }).toList();

        simulationSnapshotRepository.saveAll(snapshots);
        simulation.setStatus("COMPLETED");
        simulation.setEndDateTime(LocalDateTime.now());
        simulationRepository.save(simulation);
        return snapshots;
    }


    /**
     * Continue a completed simulation from its latest saved snapshot.
     * The additional duration is appended to the existing timeline.
     */
    @Transactional
    public List<SimulationSnapshot> continueSimulation(
            Integer simulationId,
            Integer additionalDurationMinutes) {

        Simulation simulation = getSimulation(simulationId);

        if (!"COMPLETED".equals(simulation.getStatus())) {
            throw new ApiException(
                    "Only completed simulations can be continued"
            );
        }

        if (additionalDurationMinutes == null
                || additionalDurationMinutes < 1) {
            throw new ApiException(
                    "Additional duration must be at least 1 minute"
            );
        }

        List<SimulationSnapshot> existingSnapshots =
                simulationSnapshotRepository.findBySimulation_Id(simulationId);

        if (existingSnapshots == null || existingSnapshots.isEmpty()) {
            throw new ApiException(
                    "Cannot continue a simulation without saved snapshots"
            );
        }

        int lastMinute = existingSnapshots.stream()
                .mapToInt(SimulationSnapshot::getMinute)
                .max()
                .orElseThrow();

        int newTotalDuration = lastMinute + additionalDurationMinutes;

        if (newTotalDuration > 600) {
            throw new ApiException(
                    "Total simulation duration cannot exceed 600 minutes"
            );
        }

        List<Zone> zones = zoneRepository.findAll();

        if (zones.isEmpty()) {
            throw new ApiException(
                    "Cannot continue a simulation without zones"
            );
        }

        Map<Integer, Zone> zonesById = zones.stream()
                .collect(Collectors.toMap(
                        Zone::getId,
                        Function.identity()
                ));

        Map<Integer, Integer> occupancy = new java.util.HashMap<>();
        Map<Integer, Integer> queues = new java.util.HashMap<>();

        // Find the last saved snapshot for each zone.
        for (SimulationSnapshot snapshot : existingSnapshots) {
            if (snapshot.getMinute() == lastMinute) {
                Integer zoneId = snapshot.getZone().getId();

                occupancy.put(zoneId, snapshot.getOccupancy());
                queues.put(zoneId, snapshot.getQueueLength());
            }
        }

        // Every current zone must have a saved state to resume from.
        for (Zone zone : zones) {
            if (!occupancy.containsKey(zone.getId())) {
                throw new ApiException(
                        "Missing final snapshot for zone: " + zone.getName()
                );
            }
        }

        SimulationState previousState =
                new SimulationState(occupancy, queues);

        LocalDateTime continuationStart =
                simulation.getStartDateTime().plusMinutes(lastMinute);

        LocalDateTime continuationEnd =
                continuationStart.plusMinutes(additionalDurationMinutes);

        List<ExpoEvent> events = expoEventRepository
                .findExpoEventsByOrganizer_Id(
                        simulation.getOrganizer().getId()
                )
                .stream()
                .filter(event ->
                        event.getStartDateTime().isBefore(continuationEnd)
                                && event.getEndDateTime()
                                .isAfter(continuationStart)
                )
                .toList();

        List<SimulationSnapshotRecord> records =
                eventDrivenSimulationRunner.runSimulation(
                        zones,
                        zoneConnectionRepository.findAll(),
                        events,
                        continuationStart,
                        additionalDurationMinutes,
                        30,
                        previousState
                );

        List<SimulationSnapshot> newSnapshots = records.stream()
                // Minute zero already exists in the database.
                .filter(record -> record.minute() > 0)
                .map(record -> {
                    Zone zone = zonesById.get(record.zoneId());

                    if (zone == null) {
                        throw new ApiException(
                                "Simulation produced a snapshot for an unknown zone"
                        );
                    }

                    SimulationSnapshot snapshot = new SimulationSnapshot();
                    snapshot.setSimulation(simulation);
                    snapshot.setZone(zone);
                    snapshot.setMinute(lastMinute + record.minute());
                    snapshot.setOccupancy(record.occupancy());
                    snapshot.setQueueLength(record.queueLength());

                    return snapshot;
                })
                .toList();

        simulation.setStatus("RUNNING");
        simulationRepository.save(simulation);

        simulationSnapshotRepository.saveAll(newSnapshots);

        simulation.setDurationMinutes(newTotalDuration);
        simulation.setEndDateTime(LocalDateTime.now());
        simulation.setStatus("COMPLETED");
        simulationRepository.save(simulation);

        return newSnapshots;
    }




    public void deleteSimulation(Integer id) {
        Simulation simulation = getSimulation(id);
        if (simulation.getStatus().equals("RUNNING")) {
            throw new ApiException("Cannot delete a running simulation");
        }
        simulationRepository.delete(simulation);
    }

    // Get simulation by ID
    public Simulation getSimulationById(Integer id) {
        return getSimulation(id);
    }

    // Get simulations by status
    public List<Simulation> getSimulationsByStatus(String status) {

        // Validate simulation status
        if (!List.of("PENDING", "RUNNING", "COMPLETED", "FAILED").contains(status)) {
            throw new ApiException("Invalid simulation status");
        }

        return simulationRepository.findByStatus(status);
    }

    // ===== helpers =====

    private Organizer getOrganizer(Integer organizerId) {
        Organizer organizer = organizerRepository.findOrganizerById(organizerId);
        if (organizer == null) {
            throw new ApiException("Organizer not found");
        }
        return organizer;
    }

    private Simulation getSimulation(Integer simulationId) {
        Simulation simulation = simulationRepository.findSimulationById(simulationId);
        if (simulation == null) {
            throw new ApiException("Simulation not found");
        }
        return simulation;
    }
}
