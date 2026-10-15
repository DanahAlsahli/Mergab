
package com.example.mergab.Simulation;

import com.example.mergab.Model.ExpoEvent;
import com.example.mergab.Model.Zone;
import com.example.mergab.Model.ZoneConnection;


import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;


public class EventDrivenSimulationRunner {

    private final CrowdSimulationEngine crowdSimulationEngine;
    private final EventDrivenSimulationEngine eventDrivenSimulationEngine;

    public EventDrivenSimulationRunner(
            CrowdSimulationEngine crowdSimulationEngine,
            EventDrivenSimulationEngine eventDrivenSimulationEngine) {

        this.crowdSimulationEngine =
                Objects.requireNonNull(crowdSimulationEngine);

        this.eventDrivenSimulationEngine =
                Objects.requireNonNull(eventDrivenSimulationEngine);
    }

    public List<SimulationSnapshotRecord> runSimulation(
            List<Zone> zones,
            List<ZoneConnection> connections,
            List<ExpoEvent> events,
            LocalDateTime simulationStart,
            int durationMinutes,
            int arrivalWindowMinutes) {

        validateInputs(
                zones,
                connections,
                events,
                simulationStart,
                durationMinutes,
                arrivalWindowMinutes
        );

        // Build the event arrival schedule before starting the simulation.
        Map<Integer, Map<Integer, Integer>> arrivalSchedule =
                eventDrivenSimulationEngine.scheduleArrivals(
                        events,
                        simulationStart,
                        durationMinutes,
                        arrivalWindowMinutes
                );

        Map<Integer, Integer> capacities = new HashMap<>();

        for (Zone zone : zones) {
            if (zone.getId() == null) {
                throw new IllegalArgumentException(
                        "Every zone must have an ID"
                );
            }

            if (zone.getCapacity() < 0
                    || zone.getInitialVisitors() < 0
                    || zone.getInitialVisitors() > zone.getCapacity()) {
                throw new IllegalArgumentException(
                        "Invalid initial occupancy or capacity for zone "
                                + zone.getId()
                );
            }

            capacities.put(zone.getId(), zone.getCapacity());
        }

        // Keep event arrivals waiting outside the zone separate from
        // corridor queues handled by CrowdSimulationEngine.
        Map<Integer, Integer> waitingArrivals = new HashMap<>();

        for (Zone zone : zones) {
            waitingArrivals.put(zone.getId(), 0);
        }

        SimulationState currentState = new SimulationState(
                crowdSimulationEngine.initializeOccupancy(zones),
                createEmptyQueues(zones)
        );

        List<SimulationSnapshotRecord> history = new ArrayList<>();

        // Minute zero contains the initial state, before scheduled arrivals.
        addSnapshots(history, currentState, waitingArrivals, 0);

        for (int minute = 1; minute <= durationMinutes; minute++) {

            // First, run one minute of the existing corridor movement.
            // Event arrivals from this minute are not included yet.
            List<SimulationSnapshotRecord> movementRecords =
                    crowdSimulationEngine.runSimulationWithMultipleConnections(
                            zones,
                            connections,
                            1,
                            currentState
                    );

            currentState = stateFromMinuteOne(movementRecords, zones);

            // Admit previously waiting event arrivals into newly available
            // capacity before processing new arrivals.
            currentState = admitWaitingArrivals(
                    currentState,
                    waitingArrivals,
                    capacities
            );

            // Add this minute's scheduled arrivals exactly once.
            Map<Integer, Integer> arrivalsThisMinute =
                    arrivalSchedule.getOrDefault(minute, Map.of());

            for (Map.Entry<Integer, Integer> entry
                    : arrivalsThisMinute.entrySet()) {

                Integer zoneId = entry.getKey();
                Integer arrivals = entry.getValue();

                if (!capacities.containsKey(zoneId)) {
                    throw new IllegalArgumentException(
                            "Event references unknown zone ID: " + zoneId
                    );
                }

                if (arrivals == null || arrivals < 0) {
                    throw new IllegalArgumentException(
                            "Scheduled arrivals cannot be negative"
                    );
                }

                int currentOccupancy =
                        currentState.occupancy().getOrDefault(zoneId, 0);

                int availableSpace = Math.max(
                        0,
                        capacities.get(zoneId) - currentOccupancy
                );

                int admitted = Math.min(arrivals, availableSpace);
                int waiting = arrivals - admitted;

                Map<Integer, Integer> updatedOccupancy =
                        new HashMap<>(currentState.occupancy());

                updatedOccupancy.put(
                        zoneId,
                        currentOccupancy + admitted
                );

                currentState = new SimulationState(
                        updatedOccupancy,
                        currentState.queueLengths()
                );

                waitingArrivals.merge(zoneId, waiting, Integer::sum);
            }

            // Persistable snapshot data includes both types of waiting visitors.
            addSnapshots(history, currentState, waitingArrivals, minute);
        }

        return history;
    }

    private SimulationState stateFromMinuteOne(
            List<SimulationSnapshotRecord> records,
            List<Zone> zones) {

        Map<Integer, Integer> occupancy = new HashMap<>();
        Map<Integer, Integer> queues = new HashMap<>();

        for (Zone zone : zones) {
            occupancy.put(zone.getId(), zone.getInitialVisitors());
            queues.put(zone.getId(), 0);
        }

        for (SimulationSnapshotRecord record : records) {
            if (record.minute() == 1) {
                occupancy.put(record.zoneId(), record.occupancy());
                queues.put(record.zoneId(), record.queueLength());
            }
        }

        return new SimulationState(occupancy, queues);
    }

    private SimulationState admitWaitingArrivals(
            SimulationState state,
            Map<Integer, Integer> waitingArrivals,
            Map<Integer, Integer> capacities) {

        Map<Integer, Integer> occupancy =
                new HashMap<>(state.occupancy());

        for (Map.Entry<Integer, Integer> entry
                : waitingArrivals.entrySet()) {

            int zoneId = entry.getKey();
            int waiting = entry.getValue();

            if (waiting <= 0) {
                continue;
            }

            int currentOccupancy = occupancy.getOrDefault(zoneId, 0);

            int availableSpace = Math.max(
                    0,
                    capacities.get(zoneId) - currentOccupancy
            );

            int admitted = Math.min(waiting, availableSpace);

            occupancy.put(zoneId, currentOccupancy + admitted);
            waitingArrivals.put(zoneId, waiting - admitted);
        }

        return new SimulationState(occupancy, state.queueLengths());
    }

    private Map<Integer, Integer> createEmptyQueues(List<Zone> zones) {
        Map<Integer, Integer> queues = new HashMap<>();

        for (Zone zone : zones) {
            queues.put(zone.getId(), 0);
        }

        return queues;
    }

    private void addSnapshots(
            List<SimulationSnapshotRecord> history,
            SimulationState state,
            Map<Integer, Integer> waitingArrivals,
            int minute) {

        for (Map.Entry<Integer, Integer> entry
                : state.occupancy().entrySet()) {

            int zoneId = entry.getKey();

            int totalQueue = state.queueLengths().getOrDefault(zoneId, 0)
                    + waitingArrivals.getOrDefault(zoneId, 0);

            history.add(new SimulationSnapshotRecord(
                    minute,
                    zoneId,
                    entry.getValue(),
                    totalQueue
            ));
        }
    }

    private void validateInputs(
            List<Zone> zones,
            List<ZoneConnection> connections,
            List<ExpoEvent> events,
            LocalDateTime simulationStart,
            int durationMinutes,
            int arrivalWindowMinutes) {

        if (zones == null || zones.isEmpty()) {
            throw new IllegalArgumentException(
                    "At least one zone is required"
            );
        }

        if (connections == null || events == null) {
            throw new IllegalArgumentException(
                    "Connections and events cannot be null"
            );
        }

        if (simulationStart == null) {
            throw new IllegalArgumentException(
                    "Simulation start time is required"
            );
        }

        if (durationMinutes < 1 || arrivalWindowMinutes < 1) {
            throw new IllegalArgumentException(
                    "Duration and arrival window must be positive"
            );
        }
    }
}
