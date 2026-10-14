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

        // Use the supplied state so existing queues are preserved.
        SimulationState currentState = initialState;

        // Store complete snapshots for every zone and minute.
        List<SimulationSnapshotRecord> history = new ArrayList<>();

        // Save the initial state at minute zero.
        recordSnapshot(history, currentState, 0);

        // Advance the simulation one minute at a time.
        for (int minute = 1; minute <= durationMinutes; minute++) {

            // All corridor calculations use this same starting state.
            SimulationState startOfMinute = currentState;

            // Track visitors available to leave each source zone.
            Map<Integer, Integer> remainingSourceVisitors =
                    new HashMap<>(startOfMinute.occupancy());

            // Track available space in each destination zone.
            Map<Integer, Integer> remainingDestinationSpace =
                    new HashMap<>();

            // Track waiting visitors at each destination.
            Map<Integer, Integer> remainingQueue =
                    new HashMap<>(startOfMinute.queueLengths());

            // Track remaining throughput separately for each corridor.
            Map<ZoneConnection, Integer> remainingCorridorCapacity =
                    new HashMap<>();

            // Prepare the next state without changing the starting state.
            Map<Integer, Integer> nextOccupancy =
                    new HashMap<>(startOfMinute.occupancy());

            Map<Integer, Integer> nextQueues =
                    new HashMap<>(startOfMinute.queueLengths());

            // Calculate each zone's available space and each corridor's limit.
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

                // Ensure every zone has a queue entry.
                remainingQueue.putIfAbsent(zoneId, 0);
                nextQueues.putIfAbsent(zoneId, 0);
            }

            for (ZoneConnection connection : connections) {
                remainingCorridorCapacity.put(
                        connection,
                        Math.max(0, connection.getMaxVisitorsPerMinute())
                );
            }

            // FIRST: existing queued visitors get priority.
            for (ZoneConnection connection : connections) {
                int toId = connection.getToZone().getId();

                int corridorCapacityLeft =
                        remainingCorridorCapacity.getOrDefault(
                                connection, 0);

                int waitingVisitors =
                        remainingQueue.getOrDefault(toId, 0);

                int availableSpace =
                        remainingDestinationSpace.getOrDefault(toId, 0);

                // Move queued visitors within both capacity limits.
                int queuedToMove = Math.min(
                        waitingVisitors,
                        Math.min(corridorCapacityLeft, availableSpace)
                );

                // Reduce the shared queue.
                remainingQueue.put(
                        toId,
                        waitingVisitors - queuedToMove
                );

                // Consume this corridor's throughput budget.
                remainingCorridorCapacity.put(
                        connection,
                        corridorCapacityLeft - queuedToMove
                );

                // Consume destination space.
                remainingDestinationSpace.put(
                        toId,
                        availableSpace - queuedToMove
                );

                // Update the next state.
                nextQueues.put(
                        toId,
                        nextQueues.getOrDefault(toId, 0) - queuedToMove
                );

                nextOccupancy.put(
                        toId,
                        nextOccupancy.getOrDefault(toId, 0) + queuedToMove
                );
            }

            // SECOND: process new visitors with the remaining resources.
            for (ZoneConnection connection : connections) {
                int fromId = connection.getFromZone().getId();
                int toId = connection.getToZone().getId();

                int corridorCapacityLeft =
                        remainingCorridorCapacity.getOrDefault(
                                connection, 0);

                int visitorsAvailable =
                        remainingSourceVisitors.getOrDefault(fromId, 0);

                // Each corridor requests up to 10 visitors per minute.
                int newVisitorsRequested = Math.min(
                        10,
                        Math.min(visitorsAvailable, corridorCapacityLeft)
                );

                // Consume corridor throughput, including visitors
                // who must wait because the destination is full.
                remainingCorridorCapacity.put(
                        connection,
                        corridorCapacityLeft - newVisitorsRequested
                );

                int availableSpace =
                        remainingDestinationSpace.getOrDefault(toId, 0);

                // Visitors enter only if the destination has space.
                int newVisitorsEntering = Math.min(
                        newVisitorsRequested,
                        availableSpace
                );

                // Under our queue rule, excess visitors leave the source
                // and join the destination queue.
                int newVisitorsWaiting =
                        newVisitorsRequested - newVisitorsEntering;

                int visitorsLeavingSource =
                        newVisitorsEntering + newVisitorsWaiting;

                // Prevent another corridor from reusing these visitors.
                remainingSourceVisitors.put(
                        fromId,
                        visitorsAvailable - visitorsLeavingSource
                );

                // Reserve destination space for visitors who enter.
                remainingDestinationSpace.put(
                        toId,
                        availableSpace - newVisitorsEntering
                );

                // Update source occupancy.
                nextOccupancy.put(
                        fromId,
                        nextOccupancy.getOrDefault(fromId, 0)
                                - visitorsLeavingSource
                );

                // Update destination occupancy.
                nextOccupancy.put(
                        toId,
                        nextOccupancy.getOrDefault(toId, 0)
                                + newVisitorsEntering
                );

                // Add visitors unable to enter to the destination queue.
                nextQueues.put(
                        toId,
                        nextQueues.getOrDefault(toId, 0)
                                + newVisitorsWaiting
                );
            }

            // Apply all changes together at the end of the minute.
            currentState = new SimulationState(nextOccupancy, nextQueues);

            // Save occupancy and queue lengths for every zone.
            recordSnapshot(history, currentState, minute);
        }

        return history;
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
