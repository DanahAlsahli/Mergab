package com.example.mergab.Simulation;

import com.example.mergab.Model.ExpoEvent;
import com.example.mergab.Model.Facility;
import com.example.mergab.Model.Zone;
import com.example.mergab.Model.ZoneConnection;
import com.example.mergab.Service.PrayerTimesService;

import java.time.LocalDateTime;
import java.time.LocalDate;
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

        return runSimulation(zones, connections, events, simulationStart,
                durationMinutes, arrivalWindowMinutes, 0L);
    }

    public List<SimulationSnapshotRecord> runSimulation(
            List<Zone> zones,
            List<ZoneConnection> connections,
            List<ExpoEvent> events,
            LocalDateTime simulationStart,
            int durationMinutes,
            int arrivalWindowMinutes,
            long randomSeed) {

        validateInputs(zones, connections, events, simulationStart,
                durationMinutes, arrivalWindowMinutes);
        Map<Integer, Integer> capacities = validateZones(zones);

        SimulationState initialState = new SimulationState(
                crowdSimulationEngine.initializeOccupancy(zones),
                createEmptyQueues(zones)
        );

        return runSimulationFromState(zones, connections, events,
                simulationStart, durationMinutes, arrivalWindowMinutes,
                initialState, capacities, randomSeed);
    }

    public List<SimulationSnapshotRecord> runSimulation(
            List<Zone> zones,
            List<ZoneConnection> connections,
            List<ExpoEvent> events,
            LocalDateTime simulationStart,
            int durationMinutes,
            int arrivalWindowMinutes,
            SimulationState initialState) {

        return runSimulation(zones, connections, events, simulationStart,
                durationMinutes, arrivalWindowMinutes, initialState, 0L);
    }

    public List<SimulationSnapshotRecord> runSimulation(
            List<Zone> zones,
            List<ZoneConnection> connections,
            List<ExpoEvent> events,
            LocalDateTime simulationStart,
            int durationMinutes,
            int arrivalWindowMinutes,
            SimulationState initialState,
            long randomSeed) {

        Objects.requireNonNull(initialState, "Initial state is required");
        validateInputs(zones, connections, events, simulationStart,
                durationMinutes, arrivalWindowMinutes);
        Map<Integer, Integer> capacities = validateZones(zones);

        Map<Integer, Integer> normalizedOccupancy = new HashMap<>();
        Map<Integer, Integer> normalizedQueues = new HashMap<>();
        for (Zone zone : zones) {
            int zoneId = zone.getId();
            int occupancy = initialState.occupancy().getOrDefault(zoneId, 0);
            int queue = initialState.queueLengths().getOrDefault(zoneId, 0);
            if (occupancy < 0 || occupancy > capacities.get(zoneId) || queue < 0) {
                throw new IllegalArgumentException("Invalid initial state for zone " + zoneId);
            }
            normalizedOccupancy.put(zoneId, occupancy);
            normalizedQueues.put(zoneId, queue);
        }
        if (!capacities.keySet().containsAll(initialState.occupancy().keySet())
                || !capacities.keySet().containsAll(initialState.queueLengths().keySet())) {
            throw new IllegalArgumentException("Initial state references an unknown zone ID");
        }
        SimulationState normalizedState = new SimulationState(normalizedOccupancy, normalizedQueues);

        return runSimulationFromState(zones, connections, events,
                simulationStart, durationMinutes, arrivalWindowMinutes,
                normalizedState, capacities, randomSeed);
    }

    private List<SimulationSnapshotRecord> runSimulationFromState(
            List<Zone> zones,
            List<ZoneConnection> connections,
            List<ExpoEvent> events,
            LocalDateTime simulationStart,
            int durationMinutes,
            int arrivalWindowMinutes,
            SimulationState initialState,
            Map<Integer, Integer> capacities,
            long randomSeed) {

        Map<Integer, Map<Integer, Integer>> arrivalSchedule =
                eventDrivenSimulationEngine.scheduleArrivals(
                        events, simulationStart, durationMinutes, arrivalWindowMinutes);

        boolean routeModeEnabled = hasEntranceFacility(zones);
        LocalDateTime simulationEnd = simulationStart.plusMinutes(durationMinutes);
        List<ExpoEvent> relevantEvents = events.stream()
                .filter(event -> event.getExpectedDemand() > 0)
                .filter(event -> event.getStartDateTime().minusMinutes(arrivalWindowMinutes)
                        .isBefore(simulationEnd)
                        && event.getEndDateTime().isAfter(simulationStart))
                .toList();
        Map<Integer, RoutePlan> routePlans = routeModeEnabled
                ? buildRoutePlans(zones, connections, relevantEvents, simulationEnd)
                : Map.of();
        Map<Integer, LocalDateTime> directEventEnds = new HashMap<>();
        if (!routeModeEnabled) {
            for (ExpoEvent event : relevantEvents) {
                if (event != null && event.getZone() != null && event.getZone().getId() != null) {
                    directEventEnds.merge(event.getZone().getId(), event.getEndDateTime(),
                            (a, b) -> a.isAfter(b) ? a : b);
                }
            }
        }
        Map<Integer, Integer> directEventVisitors = new HashMap<>();

        // For legacy scenarios without configured entrances, retain the existing
        // direct-to-event-zone arrival behavior.
        Map<Integer, Integer> waitingArrivals = new HashMap<>();
        for (Zone zone : zones) {
            waitingArrivals.put(zone.getId(), 0);
        }

        // In route mode, pending arrivals are grouped by their event destination.
        Map<Integer, Integer> pendingByTarget = new HashMap<>();
        PrayerMovementCoordinator prayerCoordinator = new PrayerMovementCoordinator();
        Map<LocalDate, Map<String, String>> prayerTimesByDate = new HashMap<>();
        Map<String, String> startDatePrayerTimes = getPrayerTimesForDate(
                simulationStart.toLocalDate(), prayerTimesByDate, Map.of());
        SimulationState currentState = initialState;
        OrdinaryVisitorFlow ordinaryFlow = new OrdinaryVisitorFlow(zones, initialState, randomSeed);

        // Process the simulation start instant too. The minute loop below begins
        // at 1, so without this call a prayer whose 10-minute preparation window
        // begins exactly at simulationStart would never be triggered.
        if (!startDatePrayerTimes.isEmpty()) {
            currentState = prayerCoordinator.advanceMinute(
                    currentState, zones, connections, capacities, startDatePrayerTimes,
                    simulationStart.getHour() * 60 + simulationStart.getMinute(),
                    simulationStart.toLocalDate().toString(), Map.of());
        }

        List<SimulationSnapshotRecord> history = new ArrayList<>();
        addSnapshots(history, currentState, waitingArrivals, 0);

        for (int minute = 1; minute <= durationMinutes; minute++) {
            Map<Integer, Integer> protectedVisitors = routeModeEnabled
                    ? getProtectedVisitors(routePlans)
                    : new HashMap<>();
            if (!routeModeEnabled) {
                // Legacy direct-to-event arrivals stay at their event zone until
                // the event ends, so normal movement cannot make departure counts negative.
                directEventVisitors.forEach((zoneId, count) ->
                        protectedVisitors.merge(zoneId, count, Integer::sum));
            }
            prayerCoordinator.getProtectedVisitors().forEach((zoneId, count) ->
                    protectedVisitors.merge(zoneId, count, Integer::sum));

            // Ordinary guests explore connected zones with a realistic dwell time.
            // Event-route guests and prayer guests remain protected and are handled
            // by their existing coordinators, using the same corridor budget.
            Map<ZoneConnection, Integer> corridorBudget =
                    crowdSimulationEngine.createCorridorBudget(connections);
            currentState = ordinaryFlow.advanceMinute(
                    currentState, zones, connections, capacities,
                    protectedVisitors, corridorBudget, minute);

            if (routeModeEnabled) {
                currentState = advanceRouteCohorts(
                        currentState, routePlans, zones, capacities, corridorBudget,
                        simulationStart.plusMinutes(minute), ordinaryFlow, minute);
                currentState = admitPendingRouteArrivals(
                        currentState, pendingByTarget, routePlans, capacities);
            } else {
                currentState = admitWaitingArrivals(
                        currentState, waitingArrivals, capacities, directEventVisitors);
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
                    directEventVisitors.merge(targetZoneId, admitted, Integer::sum);
                    waitingArrivals.merge(
                            targetZoneId, arrivals - admitted, Integer::sum);
                }
            }

            LocalDateTime simulatedTime = simulationStart.plusMinutes(minute);
            if (routeModeEnabled) {
                for (Map.Entry<Integer, RoutePlan> entry : routePlans.entrySet()) {
                    if (!simulatedTime.isBefore(entry.getValue().eventEndTime)) {
                        pendingByTarget.remove(entry.getKey());
                    }
                }
                waitingArrivals = routePendingQueuesByEntrance(pendingByTarget, routePlans);
            } else {
                // Legacy direct mode has no configured exit route. Remove only the
                // event attendees we tracked, not unrelated initial visitors.
                for (Map.Entry<Integer, LocalDateTime> entry : directEventEnds.entrySet()) {
                    if (!simulatedTime.isBefore(entry.getValue())) {
                        int zoneId = entry.getKey();
                        int attendees = directEventVisitors.getOrDefault(zoneId, 0);
                        // Most attendees stay to explore rather than disappearing
                        // from the Expo immediately when their event ends.
                        int departing = attendees / 5;
                        ordinaryFlow.addVisitors(zoneId, attendees - departing, minute, true);
                        currentState = adjustOccupancy(currentState, zoneId, -departing);
                        directEventVisitors.put(zoneId, 0);
                        waitingArrivals.put(zoneId, 0);
                    }
                }
            }
            Map<String, String> prayerTimes = getPrayerTimesForDate(
                    simulatedTime.toLocalDate(), prayerTimesByDate, startDatePrayerTimes);
            if (!prayerTimes.isEmpty()) {
                currentState = prayerCoordinator.advanceMinute(
                        currentState, zones, connections, capacities, prayerTimes,
                        simulatedTime.getHour() * 60 + simulatedTime.getMinute(),
                        simulatedTime.toLocalDate().toString(), protectedVisitors,
                        corridorBudget);
            }
            addSnapshots(history, currentState, waitingArrivals, minute);
        }

        return history;
    }

    private Map<Integer, RoutePlan> buildRoutePlans(
            List<Zone> zones,
            List<ZoneConnection> connections,
            List<ExpoEvent> events,
            LocalDateTime simulationEnd) {

        Map<Integer, RoutePlan> plans = new HashMap<>();
        Set<Integer> eventZoneIds = new HashSet<>();
        Map<Integer, LocalDateTime> latestEndByZone = new HashMap<>();
        for (ExpoEvent event : events) {
            if (event != null && event.getZone() != null && event.getZone().getId() != null
                    && event.getEndDateTime() != null) {
                latestEndByZone.merge(event.getZone().getId(), event.getEndDateTime(),
                        (a, b) -> a.isAfter(b) ? a : b);
            }
        }

        for (ExpoEvent event : events) {
            if (event == null || event.getZone() == null
                    || event.getZone().getId() == null) {
                throw new IllegalArgumentException(
                        "Every event must reference a zone with an ID");
            }
            int targetId = event.getZone().getId();
            LocalDateTime eventEndTime = latestEndByZone.get(targetId);
            if (!eventZoneIds.add(targetId)) continue;


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

            List<ZoneConnection> returnRoute = findRouteToExit(zones, connections, target);
            if (!eventEndTime.isAfter(simulationEnd) && returnRoute == null) {
                throw new IllegalArgumentException("Event zone " + targetId
                        + " has no directed route to an EXIT or ENTRANCE before the simulation ends");
            }
            if (returnRoute == null) returnRoute = List.of();
            List<Integer> returnZoneIds = new ArrayList<>();
            returnZoneIds.add(targetId);
            for (ZoneConnection connection : returnRoute) {
                returnZoneIds.add(connection.getToZone().getId());
            }
            plans.put(targetId, new RoutePlan(route, zoneIds, returnRoute,
                    returnZoneIds, eventEndTime));
        }
        return plans;
    }

    private List<ZoneConnection> findRouteToExit(
            List<Zone> zones, List<ZoneConnection> connections, Zone source) {
        boolean hasExplicitExit = zones.stream().filter(z -> z.getFacilities() != null)
                .flatMap(z -> z.getFacilities().stream())
                .anyMatch(f -> f != null && "EXIT".equalsIgnoreCase(f.getType()));
        String destinationType = hasExplicitExit ? "EXIT" : "ENTRANCE";
        Set<Integer> destinations = new TreeSet<>();
        for (Zone zone : zones) {
            if (zone.getFacilities() != null && zone.getFacilities().stream().anyMatch(f -> f != null
                    && destinationType.equalsIgnoreCase(f.getType()))) destinations.add(zone.getId());
        }
        if (destinations.contains(source.getId())) return List.of();
        if (destinations.isEmpty()) return null;

        Map<Integer, List<ZoneConnection>> outgoing = new HashMap<>();
        for (ZoneConnection edge : connections) {
            if (edge != null && edge.getFromZone() != null && edge.getToZone() != null
                    && edge.getFromZone().getId() != null && edge.getToZone().getId() != null) {
                outgoing.computeIfAbsent(edge.getFromZone().getId(), ignored -> new ArrayList<>()).add(edge);
            }
        }
        Queue<Integer> queue = new ArrayDeque<>();
        Set<Integer> visited = new HashSet<>();
        Map<Integer, ZoneConnection> previous = new HashMap<>();
        queue.add(source.getId()); visited.add(source.getId());
        Integer found = null;
        while (!queue.isEmpty()) {
            int current = queue.remove();
            if (destinations.contains(current)) { found = current; break; }
            for (ZoneConnection edge : outgoing.getOrDefault(current, List.of())) {
                int next = edge.getToZone().getId();
                if (visited.add(next)) { previous.put(next, edge); queue.add(next); }
            }
        }
        if (found == null) return null;
        LinkedList<ZoneConnection> path = new LinkedList<>();
        int current = found;
        while (current != source.getId()) {
            ZoneConnection edge = previous.get(current);
            if (edge == null) return null;
            path.addFirst(edge);
            current = edge.getFromZone().getId();
        }
        return path;
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
                if (count > 0) protectedCounts.merge(plan.zoneIds.get(step), count, Integer::sum);
            }
            for (int step = 0; step < plan.returningVisitorsAtStep.length; step++) {
                int count = plan.returningVisitorsAtStep[step];
                if (count > 0) protectedCounts.merge(plan.returnZoneIds.get(step), count, Integer::sum);
            }
        }
        return protectedCounts;
    }

    private SimulationState advanceRouteCohorts(
            SimulationState state,
            Map<Integer, RoutePlan> routePlans,
            List<Zone> zones,
            Map<Integer, Integer> capacities,
            Map<ZoneConnection, Integer> corridorCapacityLeft,
            LocalDateTime simulatedTime,
            OrdinaryVisitorFlow ordinaryFlow,
            int minute) {
        Map<Integer, Integer> occupancy = new HashMap<>(state.occupancy());

        for (RoutePlan plan : routePlans.values()) {
            if (!simulatedTime.isBefore(plan.eventEndTime)) {
                int lastInbound = plan.visitorsAtStep.length - 1;
                if (lastInbound >= 0 && plan.visitorsAtStep[lastInbound] > 0) {
                    int attendees = plan.visitorsAtStep[lastInbound];
                    int goingHome = Math.max(1, attendees / 5);
                    int staying = attendees - goingHome;
                    ordinaryFlow.addVisitors(plan.zoneIds.get(lastInbound), staying, minute, true);
                    plan.returningVisitorsAtStep[0] += goingHome;
                    plan.visitorsAtStep[lastInbound] = 0;
                }
            }
            int lastReturn = plan.returningVisitorsAtStep.length - 1;
            if (lastReturn >= 0 && plan.returningVisitorsAtStep[lastReturn] > 0) {
                int exited = plan.returningVisitorsAtStep[lastReturn];
                int exitZone = plan.returnZoneIds.get(lastReturn);
                occupancy.put(exitZone, Math.max(0, occupancy.getOrDefault(exitZone, 0) - exited));
                plan.returningVisitorsAtStep[lastReturn] = 0;
            }
        }

        // Inbound cohorts advance at most one directed connection per minute.
        for (RoutePlan plan : routePlans.values()) {
            for (int step = plan.connections.size() - 1; step >= 0; step--) {
                int count = plan.visitorsAtStep[step];
                if (count <= 0) continue;
                ZoneConnection edge = plan.connections.get(step);
                int from = edge.getFromZone().getId(), to = edge.getToZone().getId();
                int left = corridorCapacityLeft.getOrDefault(edge, 0);
                int free = Math.max(0, capacities.get(to) - occupancy.getOrDefault(to, 0));
                int moved = Math.min(count, Math.min(left, free));
                if (moved <= 0) continue;
                occupancy.put(from, occupancy.getOrDefault(from, 0) - moved);
                occupancy.put(to, occupancy.getOrDefault(to, 0) + moved);
                plan.visitorsAtStep[step] -= moved;
                plan.visitorsAtStep[step + 1] += moved;
                corridorCapacityLeft.put(edge, left - moved);
            }
        }

        // Event attendees return to an exit after the event ends, sharing the
        // exact same remaining per-minute corridor budget as all other cohorts.
        for (RoutePlan plan : routePlans.values()) {
            for (int step = plan.returnConnections.size() - 1; step >= 0; step--) {
                int count = plan.returningVisitorsAtStep[step];
                if (count <= 0) continue;
                ZoneConnection edge = plan.returnConnections.get(step);
                int from = edge.getFromZone().getId(), to = edge.getToZone().getId();
                int left = corridorCapacityLeft.getOrDefault(edge, 0);
                int free = Math.max(0, capacities.get(to) - occupancy.getOrDefault(to, 0));
                int moved = Math.min(count, Math.min(left, free));
                if (moved <= 0) continue;
                occupancy.put(from, occupancy.getOrDefault(from, 0) - moved);
                occupancy.put(to, occupancy.getOrDefault(to, 0) + moved);
                plan.returningVisitorsAtStep[step] -= moved;
                plan.returningVisitorsAtStep[step + 1] += moved;
                corridorCapacityLeft.put(edge, left - moved);
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

    private Map<String, String> getPrayerTimesForDate(
            LocalDate date,
            Map<LocalDate, Map<String, String>> cache,
            Map<String, String> fallback) {
        if (prayerTimesService == null) return Map.of();
        Map<String, String> cached = cache.get(date);
        if (cached != null) return cached;
        Map<String, String> times = prayerTimesService.getTimesForDate(date);
        if (times == null || times.isEmpty()) times = fallback;
        cache.put(date, times);
        return times;
    }

    private Map<Integer, Integer> validateZones(List<Zone> zones) {
        Map<Integer, Integer> capacities = new HashMap<>();
        for (Zone zone : zones) {
            if (zone == null || zone.getId() == null) {
                throw new IllegalArgumentException("Every zone must have an ID");
            }
            if (zone.getCapacity() == null || zone.getInitialVisitors() == null
                    || zone.getCapacity() < 0 || zone.getInitialVisitors() < 0
                    || zone.getInitialVisitors() > zone.getCapacity()) {
                throw new IllegalArgumentException(
                        "Invalid initial occupancy or capacity for zone " + zone.getId());
            }
            if (capacities.putIfAbsent(zone.getId(), zone.getCapacity()) != null) {
                throw new IllegalArgumentException("Duplicate zone ID: " + zone.getId());
            }
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
            Map<Integer, Integer> capacities,
            Map<Integer, Integer> directEventVisitors) {

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
            directEventVisitors.merge(zoneId, admitted, Integer::sum);
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
        private final List<ZoneConnection> returnConnections;
        private final List<Integer> returnZoneIds;
        private final int[] returningVisitorsAtStep;
        private LocalDateTime eventEndTime;

        private RoutePlan(List<ZoneConnection> connections, List<Integer> zoneIds,
                          List<ZoneConnection> returnConnections, List<Integer> returnZoneIds,
                          LocalDateTime eventEndTime) {
            this.connections = List.copyOf(connections);
            this.zoneIds = List.copyOf(zoneIds);
            this.visitorsAtStep = new int[zoneIds.size()];
            this.returnConnections = List.copyOf(returnConnections);
            this.returnZoneIds = List.copyOf(returnZoneIds);
            this.returningVisitorsAtStep = new int[returnZoneIds.size()];
            this.eventEndTime = eventEndTime;
        }
    }
}
