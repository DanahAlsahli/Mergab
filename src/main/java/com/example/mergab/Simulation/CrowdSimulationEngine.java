package com.example.mergab.Simulation;

import com.example.mergab.Model.Zone;
import com.example.mergab.Model.ZoneConnection;

import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.ArrayList;



/**
 * We'll use this order:
 * 1. Build the simulation logic in Java.
 * 2. Test its calculations with JUnit. (we use System.out.print for a fast tst)
 * 3. Connect it to Spring Boot services and repositories.
 * 4. Expose it through an API and test it with Postman.
 * 5. Connect the frontend.
 **/

public class CrowdSimulationEngine {

    // accept zones for simulations (we can have multiple zones for testing)
    public void testZones(List<Zone> zones) {

        // we can see also how many visitors in each zone
        for (Zone zone : zones) {
            // just to check how many zones it's receive only for testing don't pank
            System.out.println(
                    "Zone: " + zone.getName()
                            + ", Initial visitors: " + zone.getInitialVisitors()
                            + ", Capacity: " + zone.getCapacity()
            );
        }
    }

    // create a map for the zones in the system to track the changes for visitors count in the simulation
    public Map<Integer, Integer> initializeOccupancy(List<Zone> zones) {

        Map<Integer, Integer> occupancy = new HashMap<>();

        for (Zone zone : zones) {
            occupancy.put(zone.getId(), zone.getInitialVisitors());
        }

        return occupancy;
    }

    // print the map we created above for zones and visitors
    public void printOccupancy(Map<Integer, Integer> occupancy) {

        for (Map.Entry<Integer, Integer> entry : occupancy.entrySet()) {
            System.out.println(
                    "Zone ID: " + entry.getKey()
                            + ", Current visitors: " + entry.getValue()
            );
        }
    }

    // Rule 1 in Simulation: move visitors from one zone to another during one minute.
    // We need number of visitors available in Zone A, the connection, and then the remaining capacity in Zone B
    // Important: This method handles only one connection for one minute. We haven’t built the full simulation loop yet.
    public Map<Integer, Integer> moveVisitorsOneMinute(
            Map<Integer, Integer> currentOccupancy,
            ZoneConnection connection,
            int requestedVisitors) {

        Map<Integer, Integer> nextOccupancy =
                new HashMap<>(currentOccupancy);

        Integer fromId = connection.getFromZone().getId();
        Integer toId = connection.getToZone().getId();

        int fromCount = currentOccupancy.getOrDefault(fromId, 0);
        int toCount = currentOccupancy.getOrDefault(toId, 0);

        int targetCapacity = connection.getToZone().getCapacity();

        int allowedVisitors = Math.min(
                Math.max(requestedVisitors, 0),
                connection.getMaxVisitorsPerMinute()
        );

        int movableVisitors = Math.min(allowedVisitors, fromCount);

        int availableSpace = Math.max(0, targetCapacity - toCount);

        movableVisitors = Math.min(movableVisitors, availableSpace);

        nextOccupancy.put(fromId, fromCount - movableVisitors);
        nextOccupancy.put(toId, toCount + movableVisitors);

        return nextOccupancy;
    }


    // Move visitors while respecting the queue at the destination.
    public SimulationState processConnectionWithQueue(
            SimulationState currentState,
            ZoneConnection connection,
            int requestedVisitors) {

        // Copy the current state so we don't modify the previous snapshot.
        Map<Integer, Integer> nextOccupancy =
                new HashMap<>(currentState.occupancy());

        Map<Integer, Integer> nextQueues =
                new HashMap<>(currentState.queueLengths());

        Integer fromId = connection.getFromZone().getId();
        Integer toId = connection.getToZone().getId();

        int fromCount = currentState.occupancy().getOrDefault(fromId, 0);
        int toCount = currentState.occupancy().getOrDefault(toId, 0);

        int waitingVisitors = currentState.queueLengths().getOrDefault(toId, 0);

        int targetCapacity = connection.getToZone().getCapacity();
        int corridorCapacity = connection.getMaxVisitorsPerMinute();

        // Never allow negative requests or movement above the corridor limit.
        int allowedVisitors = Math.min(
                Math.max(requestedVisitors, 0),
                corridorCapacity
        );

        // Existing queued visitors get first access to the corridor.
        int availableSpace = Math.max(0, targetCapacity - toCount);

        int queuedToMove = Math.min(
                waitingVisitors,
                Math.min(corridorCapacity, availableSpace)
        );

        // Only visitors who actually enter leave the queue.
        nextQueues.put(toId, waitingVisitors - queuedToMove);


        // Remaining corridor capacity can be used by new visitors.
        int remainingCorridorCapacity = corridorCapacity - queuedToMove;

        // New visitors can enter the destination only if space remains.
        int remainingSpace = availableSpace - queuedToMove;

        int newVisitorsToMove = Math.min(
                allowedVisitors,
                Math.min(fromCount, remainingCorridorCapacity)
        );

        // Visitors who cannot enter the destination join its queue.
        int newVisitorsWaiting = Math.max(
                0,
                newVisitorsToMove - remainingSpace
        );

        // Only visitors who enter the destination count toward its occupancy.
        int newVisitorsEntering = newVisitorsToMove - newVisitorsWaiting;



        // Visitors who entered the corridor leave the source zone.
        nextOccupancy.put(fromId, fromCount - newVisitorsToMove);

        // Only visitors who actually enter the destination increase its occupancy.
        nextOccupancy.put(
                toId,
                toCount + queuedToMove + newVisitorsEntering
        );

        // Visitors who could not enter the destination join its existing queue.
        nextQueues.put(
                toId,
                nextQueues.getOrDefault(toId, 0) + newVisitorsWaiting
        );

        return new SimulationState(nextOccupancy, nextQueues);

    }


    // This lovely thing text the movement method above if it's work right.
    // helper method
    public void testOneMinuteMovement(
            List<Zone> zones,
            ZoneConnection connection) {

        Map<Integer, Integer> currentOccupancy =
                initializeOccupancy(zones);

        System.out.println("Before movement:");
        printOccupancy(currentOccupancy);

        Map<Integer, Integer> nextOccupancy =
                moveVisitorsOneMinute(currentOccupancy, connection, 10);

        System.out.println("After one minute:");
        printOccupancy(nextOccupancy);
    }


    // Run a simple simulation for a chosen number of minutes.
    // For our first test we will try with 10 visitors request to move every minute.


    // Run a simple simulation and collect occupancy for every minute.
    public List<OccupancyRecord> runSimpleSimulation(
            List<Zone> zones,
            ZoneConnection connection,
            int durationMinutes) {

        // Initialize occupancy and start with an empty queue for every zone.
        Map<Integer, Integer> occupancy = initializeOccupancy(zones);
        Map<Integer, Integer> queueLengths = new HashMap<>();

        for (Zone zone : zones) {
            queueLengths.put(zone.getId(), 0);
        }

        // Keep occupancy and queues together as the simulation advances.
        SimulationState currentState =
                new SimulationState(occupancy, queueLengths);

        // Store the history of all zone occupancy records.
        List<OccupancyRecord> history = new ArrayList<>();

        // Save the initial state at minute zero.
        recordOccupancy(history, currentState.occupancy(), 0);

        System.out.println("Minute 0:");
        printOccupancy(currentState.occupancy());

        for (int minute = 1; minute <= durationMinutes; minute++) {

            // Process this minute using our queue-aware movement rules.
            currentState = processConnectionWithQueue(
                    currentState,
                    connection,
                    10
            );

            // Save every zone's occupancy for this minute.
            recordOccupancy(history, currentState.occupancy(), minute);

            System.out.println("Minute " + minute + ":");
            printOccupancy(currentState.occupancy());
        }

        return history;
    }


    // Run a simulation and collect complete snapshots for every minute.
    public List<SimulationSnapshotRecord> runSimulationWithSnapshots(
            List<Zone> zones,
            ZoneConnection connection,
            int durationMinutes) {

        // Initialize occupancy and start with an empty queue for every zone.
        Map<Integer, Integer> occupancy = initializeOccupancy(zones);
        Map<Integer, Integer> queueLengths = new HashMap<>();

        for (Zone zone : zones) {
            queueLengths.put(zone.getId(), 0);
        }

        // Keep occupancy and queues together as the simulation advances.
        SimulationState currentState =
                new SimulationState(occupancy, queueLengths);

        // Store complete snapshots for all zones and minutes.
        List<SimulationSnapshotRecord> history = new ArrayList<>();

        // Save the initial state at minute zero.
        recordSnapshot(history, currentState, 0);

        for (int minute = 1; minute <= durationMinutes; minute++) {

            // Process this minute using our queue-aware movement rules.
            currentState = processConnectionWithQueue(
                    currentState,
                    connection,
                    10
            );

            // Save occupancy and queue length for every zone.
            recordSnapshot(history, currentState, minute);
        }

        return history;
    }




    // Run a simulation with multiple corridors and save complete snapshots.
    public List<SimulationSnapshotRecord> runSimulationWithMultipleConnections(
            List<Zone> zones,
            List<ZoneConnection> connections,
            int durationMinutes) {

        // Initialize occupancy and an empty queue for every zone.
        Map<Integer, Integer> occupancy = initializeOccupancy(zones);
        Map<Integer, Integer> queueLengths = new HashMap<>();

        for (Zone zone : zones) {
            queueLengths.put(zone.getId(), 0);
        }

        // Start with the default initial state for this simulation.
        SimulationState initialState =
                new SimulationState(occupancy, queueLengths);

        return runSimulationWithMultipleConnections(
                zones,
                connections,
                durationMinutes,
                initialState
        );
    }




    // Run a simulation from a supplied occupancy and queue state.
    public List<SimulationSnapshotRecord> runSimulationWithMultipleConnections(
            List<Zone> zones,
            List<ZoneConnection> connections,
            int durationMinutes,
            SimulationState initialState) {

        return runSimulationWithMultipleConnections(
                zones, connections, durationMinutes, initialState, Map.of()
        );
    }

    /**
     * Runs normal crowd movement while reserving visitors that are following
     * event-specific routes. Reserved visitors remain in occupancy but cannot
     * be selected as ordinary source visitors during this minute.
     */
    public List<SimulationSnapshotRecord> runSimulationWithMultipleConnections(
            List<Zone> zones,
            List<ZoneConnection> connections,
            int durationMinutes,
            SimulationState initialState,
            Map<Integer, Integer> reservedSourceVisitors) {

        validateSimulationInputs(zones, connections, durationMinutes,
                initialState, reservedSourceVisitors);

        SimulationState currentState = initialState;
        List<SimulationSnapshotRecord> history = new ArrayList<>();
        recordSnapshot(history, currentState, 0);

        for (int minute = 1; minute <= durationMinutes; minute++) {
            SimulationState startOfMinute = currentState;

            Map<Integer, Integer> remainingSourceVisitors =
                    new HashMap<>(startOfMinute.occupancy());

            // Keep event-routed visitors in their intended route cohort.
            for (Map.Entry<Integer, Integer> reserved
                    : reservedSourceVisitors.entrySet()) {
                int zoneId = reserved.getKey();
                int reservedCount = Math.max(0, reserved.getValue());
                remainingSourceVisitors.put(
                        zoneId,
                        Math.max(
                                0,
                                remainingSourceVisitors.getOrDefault(zoneId, 0)
                                        - reservedCount
                        )
                );
            }
            Map<Integer, Integer> remainingDestinationSpace =
                    new HashMap<>();
            Map<Integer, Integer> nextOccupancy =
                    new HashMap<>(startOfMinute.occupancy());
            Map<Integer, Integer> nextQueues =
                    new HashMap<>(startOfMinute.queueLengths());

            // Each connection has its own throughput budget for this minute.
            Map<ZoneConnection, Integer> remainingCorridorCapacity =
                    new HashMap<>();

            // Calculate available space for every zone and initialize queues.
            for (Zone zone : zones) {
                int zoneId = zone.getId();
                remainingDestinationSpace.put(
                        zoneId,
                        Math.max(
                                0,
                                zone.getCapacity()
                                        - startOfMinute.occupancy()
                                        .getOrDefault(zoneId, 0)
                        )
                );
                nextQueues.putIfAbsent(zoneId, 0);
            }

            for (ZoneConnection connection : connections) {
                remainingCorridorCapacity.put(
                        connection,
                        Math.max(0, connection.getMaxVisitorsPerMinute())
                );
            }

            // FIRST: serve each destination's shared queue only once.
            // Waiting visitors may use any incoming corridor, subject to that
            // corridor's remaining throughput and the destination's free space.
            for (Zone destination : zones) {
                int destinationId = destination.getId();
                int waitingVisitors =
                        startOfMinute.queueLengths().getOrDefault(destinationId, 0);
                int queueServed = 0;

                for (ZoneConnection connection : connections) {
                    if (waitingVisitors == 0) {
                        break;
                    }

                    if (!connection.getToZone().getId().equals(destinationId)) {
                        continue;
                    }

                    int corridorCapacityLeft =
                            remainingCorridorCapacity.getOrDefault(connection, 0);
                    int availableSpace =
                            remainingDestinationSpace.getOrDefault(destinationId, 0);

                    int queuedToMove = Math.min(
                            waitingVisitors,
                            Math.min(corridorCapacityLeft, availableSpace)
                    );

                    if (queuedToMove <= 0) {
                        continue;
                    }

                    waitingVisitors -= queuedToMove;
                    queueServed += queuedToMove;

                    remainingCorridorCapacity.put(
                            connection,
                            corridorCapacityLeft - queuedToMove
                    );
                    remainingDestinationSpace.put(
                            destinationId,
                            availableSpace - queuedToMove
                    );
                    nextOccupancy.put(
                            destinationId,
                            nextOccupancy.getOrDefault(destinationId, 0) + queuedToMove
                    );
                }

                // Remove only the queued visitors who actually entered.
                nextQueues.put(
                        destinationId,
                        startOfMinute.queueLengths().getOrDefault(destinationId, 0)
                                - queueServed
                );
            }

            // SECOND: move new visitors using each corridor's remaining capacity.
            for (ZoneConnection connection : connections) {
                int fromId = connection.getFromZone().getId();
                int toId = connection.getToZone().getId();

                int corridorCapacityLeft =
                        remainingCorridorCapacity.getOrDefault(connection, 0);
                int visitorsAvailable =
                        remainingSourceVisitors.getOrDefault(fromId, 0);

                // Existing queues have priority; request up to 10 new visitors.
                int newVisitorsRequested = Math.min(
                        10,
                        Math.min(visitorsAvailable, corridorCapacityLeft)
                );

                if (newVisitorsRequested <= 0) {
                    continue;
                }

                remainingCorridorCapacity.put(
                        connection,
                        corridorCapacityLeft - newVisitorsRequested
                );

                int availableSpace =
                        remainingDestinationSpace.getOrDefault(toId, 0);
                int newVisitorsEntering =
                        Math.min(newVisitorsRequested, availableSpace);
                int newVisitorsWaiting =
                        newVisitorsRequested - newVisitorsEntering;

                // Under the current queue rule, visitors unable to enter leave
                // the source and join the destination's shared queue.
                remainingSourceVisitors.put(
                        fromId,
                        visitorsAvailable - newVisitorsRequested
                );
                remainingDestinationSpace.put(
                        toId,
                        availableSpace - newVisitorsEntering
                );

                nextOccupancy.put(
                        fromId,
                        nextOccupancy.getOrDefault(fromId, 0) - newVisitorsRequested
                );
                nextOccupancy.put(
                        toId,
                        nextOccupancy.getOrDefault(toId, 0) + newVisitorsEntering
                );
                nextQueues.put(
                        toId,
                        nextQueues.getOrDefault(toId, 0) + newVisitorsWaiting
                );
            }

            currentState = new SimulationState(nextOccupancy, nextQueues);
            recordSnapshot(history, currentState, minute);
        }

        return history;
    }


    /**
     * Reject invalid simulation inputs before creating snapshots or moving visitors.
     * This keeps impossible occupancy and queue values from contaminating every
     * later minute of the simulation.
     */
    private void validateSimulationInputs(
            List<Zone> zones,
            List<ZoneConnection> connections,
            int durationMinutes,
            SimulationState initialState,
            Map<Integer, Integer> reservedSourceVisitors) {

        if (zones == null || zones.isEmpty()) {
            throw new IllegalArgumentException("Zones cannot be null or empty");
        }
        if (connections == null) {
            throw new IllegalArgumentException("Connections cannot be null");
        }
        if (initialState == null) {
            throw new IllegalArgumentException("Initial simulation state cannot be null");
        }
        if (reservedSourceVisitors == null) {
            throw new IllegalArgumentException("Reserved visitor counts cannot be null");
        }
        if (durationMinutes < 0) {
            throw new IllegalArgumentException("Simulation duration cannot be negative");
        }

        Map<Integer, Zone> zonesById = new HashMap<>();
        for (Zone zone : zones) {
            if (zone == null || zone.getId() == null) {
                throw new IllegalArgumentException("Every zone must have an ID");
            }
            if (zone.getCapacity() == null || zone.getCapacity() < 0) {
                throw new IllegalArgumentException(
                        "Zone capacity cannot be null or negative: " + zone.getId());
            }
            if (zonesById.putIfAbsent(zone.getId(), zone) != null) {
                throw new IllegalArgumentException("Duplicate zone ID: " + zone.getId());
            }
        }

        for (Map.Entry<Integer, Integer> entry : initialState.occupancy().entrySet()) {
            Integer zoneId = entry.getKey();
            Integer count = entry.getValue();
            Zone zone = zonesById.get(zoneId);
            if (zone == null) {
                throw new IllegalArgumentException("Initial occupancy references unknown zone: " + zoneId);
            }
            if (count == null || count < 0) {
                throw new IllegalArgumentException("Occupancy cannot be negative or null for zone: " + zoneId);
            }
            if (count > zone.getCapacity()) {
                throw new IllegalArgumentException("Initial occupancy exceeds capacity for zone: " + zoneId);
            }
        }

        for (Map.Entry<Integer, Integer> entry : initialState.queueLengths().entrySet()) {
            Integer zoneId = entry.getKey();
            Integer count = entry.getValue();
            if (!zonesById.containsKey(zoneId)) {
                throw new IllegalArgumentException("Initial queue references unknown zone: " + zoneId);
            }
            if (count == null || count < 0) {
                throw new IllegalArgumentException("Queue length cannot be negative or null for zone: " + zoneId);
            }
        }

        for (ZoneConnection connection : connections) {
            if (connection == null || connection.getFromZone() == null
                    || connection.getToZone() == null
                    || connection.getFromZone().getId() == null
                    || connection.getToZone().getId() == null) {
                throw new IllegalArgumentException("Every connection must have valid source and destination zones");
            }
            if (!zonesById.containsKey(connection.getFromZone().getId())
                    || !zonesById.containsKey(connection.getToZone().getId())) {
                throw new IllegalArgumentException("Connection references a zone outside this simulation");
            }
            if (connection.getMaxVisitorsPerMinute() == null
                    || connection.getMaxVisitorsPerMinute() <= 0) {
                throw new IllegalArgumentException("Corridor throughput must be greater than zero");
            }
        }

        for (Map.Entry<Integer, Integer> entry : reservedSourceVisitors.entrySet()) {
            if (!zonesById.containsKey(entry.getKey())) {
                throw new IllegalArgumentException("Reserved visitors reference unknown zone: " + entry.getKey());
            }
            if (entry.getValue() == null || entry.getValue() < 0) {
                throw new IllegalArgumentException("Reserved visitor count cannot be negative or null");
            }
        }
    }


    // Save one occupancy record for each zone at the current minute.
    private void recordOccupancy(
            List<OccupancyRecord> history,
            Map<Integer, Integer> occupancy,
            int minute) {

        for (Map.Entry<Integer, Integer> entry : occupancy.entrySet()) {
            history.add(new OccupancyRecord(
                    minute,
                    entry.getKey(),
                    entry.getValue()
            ));
        }
    }


    /**
    What does this helper do?
    For every zone, it creates a snapshot containing:
    - minute — when the snapshot was taken.
    - zoneId — which zone it describes.
    - occupancy — how many visitors are inside.
    - queueLength — how many visitors are waiting.
     */
    // Save occupancy and queue length for every zone at a specific minute.
    private void recordSnapshot(
            List<SimulationSnapshotRecord> history,
            SimulationState state,
            int minute) {

        // Record the complete state of each zone at this minute.
        for (Map.Entry<Integer, Integer> entry
                : state.occupancy().entrySet()) {

            int zoneId = entry.getKey();
            int occupancy = entry.getValue();

            // If a zone has no recorded queue, treat it as an empty queue.
            int queueLength = state.queueLengths().getOrDefault(zoneId, 0);

            history.add(new SimulationSnapshotRecord(
                    minute,
                    zoneId,
                    occupancy,
                    queueLength
            ));
        }
    }




    // Describe how visitors move through one corridor during one minute.
    private record CorridorMovement(
            int fromZoneId,
            int toZoneId,
            int queuedVisitorsEntering,
            int newVisitorsEntering,
            int newVisitorsJoiningQueue
    ) {
    }

    // Calculate one corridor's movement without changing the current state.
    private CorridorMovement calculateCorridorMovement(
            SimulationState startOfMinute,
            ZoneConnection connection,
            int requestedVisitors,
            int remainingDestinationSpace) {

        int fromId = connection.getFromZone().getId();
        int toId = connection.getToZone().getId();

        int fromCount =
                startOfMinute.occupancy().getOrDefault(fromId, 0);

        int waitingVisitors =
                startOfMinute.queueLengths().getOrDefault(toId, 0);

        int corridorCapacity = connection.getMaxVisitorsPerMinute();

        // Existing queued visitors have priority.
        int queuedVisitorsEntering = Math.min(
                waitingVisitors,
                Math.min(corridorCapacity, remainingDestinationSpace)
        );

        // Calculate the corridor capacity left for new visitors.
        int remainingCorridorCapacity =
                corridorCapacity - queuedVisitorsEntering;

        int remainingSpace =
                remainingDestinationSpace - queuedVisitorsEntering;

        // Limit new arrivals by the request, corridor capacity, and source occupancy.
        int newVisitorsRequested = Math.min(
                Math.max(requestedVisitors, 0),
                Math.min(fromCount, remainingCorridorCapacity)
        );

        // Some visitors may leave the source zone but wait at the destination entrance.
        int newVisitorsEntering = Math.min(
                newVisitorsRequested,
                remainingSpace
        );

        int newVisitorsJoiningQueue =
                newVisitorsRequested - newVisitorsEntering;

        return new CorridorMovement(
                fromId,
                toId,
                queuedVisitorsEntering,
                newVisitorsEntering,
                newVisitorsJoiningQueue
        );
    }







} // class end
