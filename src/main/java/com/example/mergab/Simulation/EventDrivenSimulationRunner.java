package com.example.mergab.Simulation;

import com.example.mergab.Model.ExpoEvent;
import com.example.mergab.Model.Facility;
import com.example.mergab.Model.Zone;
import com.example.mergab.Model.ZoneConnection;
import com.example.mergab.Service.PrayerTimesService;

import java.time.LocalDateTime;
import java.util.*;

public class EventDrivenSimulationRunner {

    private final CrowdSimulationEngine crowdSimulationEngine;
    private final EventDrivenSimulationEngine eventDrivenSimulationEngine;
    private final EventRouteFinder eventRouteFinder;
    private final PrayerTimesService prayerTimesService;

    public EventDrivenSimulationRunner(
            CrowdSimulationEngine crowdSimulationEngine,
            EventDrivenSimulationEngine eventDrivenSimulationEngine) {
        this.crowdSimulationEngine = Objects.requireNonNull(crowdSimulationEngine);
        this.eventDrivenSimulationEngine = Objects.requireNonNull(eventDrivenSimulationEngine);
        this.eventRouteFinder = new EventRouteFinder();
        this.prayerTimesService = null;
    }

    public EventDrivenSimulationRunner(
            CrowdSimulationEngine crowdSimulationEngine,
            EventDrivenSimulationEngine eventDrivenSimulationEngine,
            PrayerTimesService prayerTimesService) {
        this.crowdSimulationEngine = Objects.requireNonNull(crowdSimulationEngine);
        this.eventDrivenSimulationEngine = Objects.requireNonNull(eventDrivenSimulationEngine);
        this.eventRouteFinder = new EventRouteFinder();
        this.prayerTimesService = Objects.requireNonNull(prayerTimesService);
    }

    public List<SimulationSnapshotRecord> runSimulation(
            List<Zone> zones,
            List<ZoneConnection> connections,
            List<ExpoEvent> events,
            LocalDateTime simulationStart,
            int durationMinutes,
            int arrivalWindowMinutes) {

        validateInputs(zones, connections, events, simulationStart,
                durationMinutes, arrivalWindowMinutes);
        Map<Integer, Integer> capacities = validateZones(zones);

        SimulationState initialState = new SimulationState(
                crowdSimulationEngine.initializeOccupancy(zones),
                createEmptyQueues(zones)
        );

        return runSimulationFromState(zones, connections, events,
                simulationStart, durationMinutes, arrivalWindowMinutes,
                initialState, capacities);
    }

    public List<SimulationSnapshotRecord> runSimulation(
            List<Zone> zones,
            List<ZoneConnection> connections,
            List<ExpoEvent> events,
            LocalDateTime simulationStart,
            int durationMinutes,
            int arrivalWindowMinutes,
            SimulationState initialState) {

        Objects.requireNonNull(initialState, "Initial state is required");
        validateInputs(zones, connections, events, simulationStart,
                durationMinutes, arrivalWindowMinutes);
        Map<Integer, Integer> capacities = validateZones(zones);

        for (Zone zone : zones) {
            int zoneId = zone.getId();
            int occupancy = initialState.occupancy().getOrDefault(zoneId, 0);
            int queue = initialState.queueLengths().getOrDefault(zoneId, 0);
            if (occupancy < 0 || occupancy > capacities.get(zoneId) || queue < 0) {
                throw new IllegalArgumentException(
                        "Invalid initial state for zone " + zoneId);
            }
        }

        return runSimulationFromState(zones, connections, events,
                simulationStart, durationMinutes, arrivalWindowMinutes,
                initialState, capacities);
    }

    private List<SimulationSnapshotRecord> runSimulationFromState(
            List<Zone> zones,
            List<ZoneConnection> connections,
            List<ExpoEvent> events,
            LocalDateTime simulationStart,
            int durationMinutes,
            int arrivalWindowMinutes,
            SimulationState initialState,
            Map<Integer, Integer> capacities) {

        Map<Integer, Map<Integer, Integer>> arrivalSchedule =
                eventDrivenSimulationEngine.scheduleArrivals(
                        events, simulationStart, durationMinutes, arrivalWindowMinutes);

        boolean routeModeEnabled = hasEntranceFacility(zones);
        Map<Integer, RoutePlan> routePlans = routeModeEnabled
                ? buildRoutePlans(zones, connections, events)
                : Map.of();

        // For legacy scenarios without configured entrances, retain the existing
        // direct-to-event-zone arrival behavior.
        Map<Integer, Integer> waitingArrivals = new HashMap<>();
        for (Zone zone : zones) {
            waitingArrivals.put(zone.getId(), 0);
        }

        // In route mode, pending arrivals are grouped by their event destination.
        Map<Integer, Integer> pendingByTarget = new HashMap<>();
        PrayerMovementCoordinator prayerCoordinator = new PrayerMovementCoordinator();
        Map<String, String> prayerTimes = prayerTimesService == null
                ? Map.of() : prayerTimesService.getTodayTimes();
        SimulationState currentState = initialState;

        // Process the simulation start instant too. The minute loop below begins
        // at 1, so without this call a prayer whose 10-minute preparation window
        // begins exactly at simulationStart would never be triggered.
        if (!prayerTimes.isEmpty()) {
            currentState = prayerCoordinator.advanceMinute(
                    currentState, zones, connections, capacities, prayerTimes,
                    simulationStart.getHour() * 60 + simulationStart.getMinute(),
                    simulationStart.toLocalDate().toString(), Map.of());
        }

        List<SimulationSnapshotRecord> history = new ArrayList<>();
        addSnapshots(history, currentState, waitingArrivals, 0);

        for (int minute = 1; minute <= durationMinutes; minute++) {
            Map<Integer, Integer> protectedVisitors = routeModeEnabled
                    ? getProtectedVisitors(routePlans)
                    : new HashMap<>();
            prayerCoordinator.getProtectedVisitors().forEach((zoneId, count) ->
                    protectedVisitors.merge(zoneId, count, Integer::sum));

            List<SimulationSnapshotRecord> movementRecords =
                    crowdSimulationEngine.runSimulationWithMultipleConnections(
                            zones, connections, 1, currentState, protectedVisitors);

            currentState = stateFromMinuteOne(movementRecords, zones, currentState);

            if (routeModeEnabled) {
                currentState = advanceRouteCohorts(
                        currentState, routePlans, zones, capacities);
                currentState = admitPendingRouteArrivals(
                        currentState, pendingByTarget, routePlans, capacities);
            } else {
                currentState = admitWaitingArrivals(
                        currentState, waitingArrivals, capacities);
            }

            Map<Integer, Integer> arrivalsThisMinute =
                    arrivalSchedule.getOrDefault(minute, Map.of());

            for (Map.Entry<Integer, Integer> entry : arrivalsThisMinute.entrySet()) {
                Integer targetZoneId = entry.getKey();
                Integer arrivals = entry.getValue();

                if (!capacities.containsKey(targetZoneId)) {
                    throw new IllegalArgumentException(
                            "Event references unknown zone ID: " + targetZoneId);
                }
                if (arrivals == null || arrivals < 0) {
                    throw new IllegalArgumentException(
                            "Scheduled arrivals cannot be negative");
                }

                if (routeModeEnabled) {
                    RoutePlan plan = routePlans.get(targetZoneId);
                    if (plan == null) {
                        throw new IllegalArgumentException(
                                "No entrance route found for event zone ID: "
                                        + targetZoneId);
                    }

                    int entranceZoneId = plan.zoneIds.get(0);
                    int occupancy = currentState.occupancy()
                            .getOrDefault(entranceZoneId, 0);
                    int availableSpace = Math.max(
                            0, capacities.get(entranceZoneId) - occupancy);
                    int admitted = Math.min(arrivals, availableSpace);

                    currentState = adjustOccupancy(
                            currentState, entranceZoneId, admitted);
                    plan.visitorsAtStep[0] += admitted;
                    pendingByTarget.merge(
                            targetZoneId, arrivals - admitted, Integer::sum);
                } else {
                    int occupancy = currentState.occupancy()
                            .getOrDefault(targetZoneId, 0);
                    int availableSpace = Math.max(
                            0, capacities.get(targetZoneId) - occupancy);
                    int admitted = Math.min(arrivals, availableSpace);

                    currentState = adjustOccupancy(
                            currentState, targetZoneId, admitted);
                    waitingArrivals.merge(
                            targetZoneId, arrivals - admitted, Integer::sum);
                }
            }

            if (routeModeEnabled) {
                waitingArrivals = routePendingQueuesByEntrance(
                        pendingByTarget, routePlans);
            }
            if (!prayerTimes.isEmpty()) {
                LocalDateTime simulatedTime = simulationStart.plusMinutes(minute);
                currentState = prayerCoordinator.advanceMinute(
                        currentState, zones, connections, capacities, prayerTimes,
                        simulatedTime.getHour() * 60 + simulatedTime.getMinute(),
                        simulatedTime.toLocalDate().toString(), protectedVisitors);
            }
            addSnapshots(history, currentState, waitingArrivals, minute);
        }

        return history;
    }

    private Map<Integer, RoutePlan> buildRoutePlans(
            List<Zone> zones,
            List<ZoneConnection> connections,
            List<ExpoEvent> events) {

        Map<Integer, RoutePlan> plans = new HashMap<>();
        Set<Integer> eventZoneIds = new HashSet<>();

        for (ExpoEvent event : events) {
            if (event == null || event.getZone() == null
                    || event.getZone().getId() == null) {
                throw new IllegalArgumentException(
                        "Every event must reference a zone with an ID");
            }
            int targetId = event.getZone().getId();
            if (!eventZoneIds.add(targetId)) {
                continue;
            }


            Zone target = zones.stream()
                    .filter(zone -> Objects.equals(targetId, zone.getId()))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Event references unknown zone ID: " + targetId));


            List<ZoneConnection> route = eventRouteFinder
                    .findRoute(zones, connections, target)
                    .orElseThrow(() -> new IllegalArgumentException(
                            "No directed route from an ENTRANCE to event zone ID: "
                                    + targetId));

            List<Integer> zoneIds = new ArrayList<>();
            if (route.isEmpty()) {
                // An entrance can also be the event's destination.
                zoneIds.add(targetId);
            } else {
                zoneIds.add(route.get(0).getFromZone().getId());
                for (ZoneConnection connection : route) {
                    zoneIds.add(connection.getToZone().getId());
                }
            }

            plans.put(targetId, new RoutePlan(route, zoneIds));
        }
        return plans;
    }

    private boolean hasEntranceFacility(List<Zone> zones) {
        for (Zone zone : zones) {
            if (zone.getFacilities() == null) {
                continue;
            }
            for (Facility facility : zone.getFacilities()) {
                if (facility != null
                        && "ENTRANCE".equalsIgnoreCase(facility.getType())) {
                    return true;
                }
            }
        }
        return false;
    }

    private Map<Integer, Integer> getProtectedVisitors(
            Map<Integer, RoutePlan> routePlans) {
        Map<Integer, Integer> protectedCounts = new HashMap<>();
        for (RoutePlan plan : routePlans.values()) {
            for (int step = 0; step < plan.visitorsAtStep.length; step++) {
                int count = plan.visitorsAtStep[step];
                if (count > 0) {
                    protectedCounts.merge(plan.zoneIds.get(step), count, Integer::sum);
                }
            }
        }
        return protectedCounts;
    }

    private SimulationState advanceRouteCohorts(
            SimulationState state,
            Map<Integer, RoutePlan> routePlans,
            List<Zone> zones,
            Map<Integer, Integer> capacities) {

        Map<Integer, Integer> occupancy = new HashMap<>(state.occupancy());
        Map<ZoneConnection, Integer> corridorCapacityLeft = new HashMap<>();
        Map<Integer, Integer> zoneCapacity = capacities;

        for (RoutePlan plan : routePlans.values()) {
            for (ZoneConnection connection : plan.connections) {
                corridorCapacityLeft.putIfAbsent(
                        connection,
                        Math.max(0, connection.getMaxVisitorsPerMinute()));
            }
        }

        // Each cohort advances by at most one connection per minute.
        for (RoutePlan plan : routePlans.values()) {
            // Work backwards so a cohort moved into the next zone cannot
            // traverse a second connection during the same minute.
            for (int step = plan.connections.size() - 1; step >= 0; step--) {
                int waitingAtStep = plan.visitorsAtStep[step];
                if (waitingAtStep <= 0) {
                    continue;
                }

                ZoneConnection connection = plan.connections.get(step);
                int fromId = connection.getFromZone().getId();
                int toId = connection.getToZone().getId();

                int corridorLeft = corridorCapacityLeft.getOrDefault(connection, 0);
                int freeSpace = Math.max(
                        0, zoneCapacity.get(toId) - occupancy.getOrDefault(toId, 0));
                int moved = Math.min(waitingAtStep, Math.min(corridorLeft, freeSpace));

                if (moved <= 0) {
                    continue;
                }

                occupancy.put(fromId, occupancy.getOrDefault(fromId, 0) - moved);
                occupancy.put(toId, occupancy.getOrDefault(toId, 0) + moved);
                plan.visitorsAtStep[step] -= moved;
                plan.visitorsAtStep[step + 1] += moved;
                corridorCapacityLeft.put(connection, corridorLeft - moved);
            }
        }

        return new SimulationState(occupancy, state.queueLengths());
    }

    private SimulationState admitPendingRouteArrivals(
            SimulationState state,
            Map<Integer, Integer> pendingByTarget,
            Map<Integer, RoutePlan> routePlans,
            Map<Integer, Integer> capacities) {

        SimulationState current = state;
        for (Map.Entry<Integer, Integer> entry
                : new ArrayList<>(pendingByTarget.entrySet())) {
            int targetId = entry.getKey();
            int pending = entry.getValue();
            if (pending <= 0) {
                continue;
            }

            RoutePlan plan = routePlans.get(targetId);
            int entranceId = plan.zoneIds.get(0);
            int occupancy = current.occupancy().getOrDefault(entranceId, 0);
            int freeSpace = Math.max(0, capacities.get(entranceId) - occupancy);
            int admitted = Math.min(pending, freeSpace);

            if (admitted > 0) {
                current = adjustOccupancy(current, entranceId, admitted);
                plan.visitorsAtStep[0] += admitted;
                pendingByTarget.put(targetId, pending - admitted);
            }
        }
        return current;
    }

    private Map<Integer, Integer> routePendingQueuesByEntrance(
            Map<Integer, Integer> pendingByTarget,
            Map<Integer, RoutePlan> routePlans) {
        Map<Integer, Integer> queuesByEntrance = new HashMap<>();
        for (Map.Entry<Integer, Integer> entry : pendingByTarget.entrySet()) {
            if (entry.getValue() <= 0) {
                continue;
            }
            RoutePlan plan = routePlans.get(entry.getKey());
            queuesByEntrance.merge(
                    plan.zoneIds.get(0), entry.getValue(), Integer::sum);
        }
        return queuesByEntrance;
    }

    private SimulationState adjustOccupancy(
            SimulationState state, int zoneId, int delta) {
        if (delta == 0) {
            return state;
        }
        Map<Integer, Integer> occupancy = new HashMap<>(state.occupancy());
        occupancy.put(zoneId, occupancy.getOrDefault(zoneId, 0) + delta);
        return new SimulationState(occupancy, state.queueLengths());
    }

    private Map<Integer, Integer> validateZones(List<Zone> zones) {
        Map<Integer, Integer> capacities = new HashMap<>();
        for (Zone zone : zones) {
            if (zone.getId() == null) {
                throw new IllegalArgumentException("Every zone must have an ID");
            }
            if (zone.getCapacity() == null || zone.getInitialVisitors() == null
                    || zone.getCapacity() < 0 || zone.getInitialVisitors() < 0
                    || zone.getInitialVisitors() > zone.getCapacity()) {
                throw new IllegalArgumentException(
                        "Invalid initial occupancy or capacity for zone " + zone.getId());
            }
            capacities.put(zone.getId(), zone.getCapacity());
        }
        return capacities;
    }

    private SimulationState stateFromMinuteOne(
            List<SimulationSnapshotRecord> records,
            List<Zone> zones,
            SimulationState previousState) {

        Map<Integer, Integer> occupancy = new HashMap<>(previousState.occupancy());
        Map<Integer, Integer> queues = new HashMap<>(previousState.queueLengths());

        for (Zone zone : zones) {
            occupancy.putIfAbsent(zone.getId(), zone.getInitialVisitors());
            queues.putIfAbsent(zone.getId(), 0);
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

        Map<Integer, Integer> occupancy = new HashMap<>(state.occupancy());
        for (Map.Entry<Integer, Integer> entry : waitingArrivals.entrySet()) {
            int zoneId = entry.getKey();
            int waiting = entry.getValue();
            if (waiting <= 0) {
                continue;
            }
            int currentOccupancy = occupancy.getOrDefault(zoneId, 0);
            int availableSpace = Math.max(0, capacities.get(zoneId) - currentOccupancy);
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
        for (Map.Entry<Integer, Integer> entry : state.occupancy().entrySet()) {
            int zoneId = entry.getKey();
            int totalQueue = state.queueLengths().getOrDefault(zoneId, 0)
                    + waitingArrivals.getOrDefault(zoneId, 0);
            history.add(new SimulationSnapshotRecord(
                    minute, zoneId, entry.getValue(), totalQueue));
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
            throw new IllegalArgumentException("At least one zone is required");
        }
        if (connections == null || events == null) {
            throw new IllegalArgumentException("Connections and events cannot be null");
        }
        if (simulationStart == null) {
            throw new IllegalArgumentException("Simulation start time is required");
        }
        if (durationMinutes < 1 || arrivalWindowMinutes < 1) {
            throw new IllegalArgumentException("Duration and arrival window must be positive");
        }
    }

    private static class RoutePlan {
        private final List<ZoneConnection> connections;
        private final List<Integer> zoneIds;
        private final int[] visitorsAtStep;

        private RoutePlan(List<ZoneConnection> connections, List<Integer> zoneIds) {
            this.connections = List.copyOf(connections);
            this.zoneIds = List.copyOf(zoneIds);
            this.visitorsAtStep = new int[zoneIds.size()];
        }
    }
}
