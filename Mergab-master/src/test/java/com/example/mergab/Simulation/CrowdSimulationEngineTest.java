package com.example.mergab.Simulation;


import com.example.mergab.Model.Zone;
import com.example.mergab.Model.ZoneConnection;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// for testing the simulation calculation and making sure everything is ok


public class CrowdSimulationEngineTest {


    // test simulation on 2 Zones A and B
    // move 10 visitors every 1 minutes from A to B
    // this test for 1 minute only
    @Test
    void shouldMoveTenVisitorsFromOneZoneToAnother() {

        // Create Zone A with 80 visitors and capacity 100.
        Zone zoneA = new Zone();
        zoneA.setId(1);
        zoneA.setName("Zone A");
        zoneA.setCapacity(100);
        zoneA.setInitialVisitors(80);

        // Create Zone B with 30 visitors and capacity 150.
        Zone zoneB = new Zone();
        zoneB.setId(2);
        zoneB.setName("Zone B");
        zoneB.setCapacity(150);
        zoneB.setInitialVisitors(30);

        // Create a connection from Zone A to Zone B.
        ZoneConnection connection = new ZoneConnection();
        connection.setId(1);
        connection.setFromZone(zoneA);
        connection.setToZone(zoneB);
        connection.setMaxVisitorsPerMinute(10);

        // Create our simulation engine.
        CrowdSimulationEngine engine = new CrowdSimulationEngine();

        // Initialize the visitor counts.
        Map<Integer, Integer> currentOccupancy =
                engine.initializeOccupancy(List.of(zoneA, zoneB));

        // Move visitors for one minute.
        Map<Integer, Integer> nextOccupancy =
                engine.moveVisitorsOneMinute(currentOccupancy, connection, 10);

        // Check that the visitor counts are correct.
        assertEquals(70, nextOccupancy.get(1));
        assertEquals(40, nextOccupancy.get(2));

        // Check that the total number of visitors stays the same.
        assertEquals(
                110,
                nextOccupancy.get(1) + nextOccupancy.get(2)
        );
    }



    // this test for 5 minutes
    @Test
    void shouldSimulateFiveMinutesCorrectly() {

        // Create Zone A with 80 visitors and capacity 100.
        Zone zoneA = new Zone();
        zoneA.setId(1);
        zoneA.setName("Zone A");
        zoneA.setCapacity(100);
        zoneA.setInitialVisitors(80);

        // Create Zone B with 30 visitors and capacity 150.
        Zone zoneB = new Zone();
        zoneB.setId(2);
        zoneB.setName("Zone B");
        zoneB.setCapacity(150);
        zoneB.setInitialVisitors(30);

        // Create a connection from Zone A to Zone B.
        ZoneConnection connection = new ZoneConnection();
        connection.setId(1);
        connection.setFromZone(zoneA);
        connection.setToZone(zoneB);
        connection.setMaxVisitorsPerMinute(10);

        // Create our simulation engine.
        CrowdSimulationEngine engine = new CrowdSimulationEngine();

        // Initialize the visitor counts at minute 0.
        Map<Integer, Integer> occupancy =
                engine.initializeOccupancy(List.of(zoneA, zoneB));

        // Check the starting occupancy.
        assertEquals(80, occupancy.get(1));
        assertEquals(30, occupancy.get(2));

        // Run the simulation one minute at a time.
        for (int minute = 1; minute <= 5; minute++) {

            occupancy = engine.moveVisitorsOneMinute(
                    occupancy, connection, 10
            );

            // Check the expected occupancy after each minute.
            assertEquals(80 - (minute * 10), occupancy.get(1));
            assertEquals(30 + (minute * 10), occupancy.get(2));

            // The total number of visitors must remain 110.
            assertEquals(
                    110,
                    occupancy.get(1) + occupancy.get(2)
            );
        }
    }


    // this will show outputs
    @Test
    void shouldRunSimpleSimulationForFiveMinutes() {

        // Create Zone A with 80 visitors and capacity 100.
        Zone zoneA = new Zone();
        zoneA.setId(1);
        zoneA.setName("Zone A");
        zoneA.setCapacity(100);
        zoneA.setInitialVisitors(80);

        // Create Zone B with 30 visitors and capacity 150.
        Zone zoneB = new Zone();
        zoneB.setId(2);
        zoneB.setName("Zone B");
        zoneB.setCapacity(150);
        zoneB.setInitialVisitors(30);

        // Create a connection from Zone A to Zone B.
        ZoneConnection connection = new ZoneConnection();
        connection.setId(1);
        connection.setFromZone(zoneA);
        connection.setToZone(zoneB);
        connection.setMaxVisitorsPerMinute(10);

        // Create our simulation engine.
        CrowdSimulationEngine engine = new CrowdSimulationEngine();

        // Capture the printed output so JUnit can check it.
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        PrintStream originalOutput = System.out;

        try {
            System.setOut(new PrintStream(output, true, StandardCharsets.UTF_8));

            // Run the actual five-minute simulation.
            engine.runSimpleSimulation(List.of(zoneA, zoneB), connection, 5);

        } finally {
            // Restore normal console output after the test.
            System.setOut(originalOutput);
        }

        String result = output.toString(StandardCharsets.UTF_8);

        // Check the starting occupancy.
        assertTrue(result.contains("Minute 0:"));
        assertTrue(result.contains("Zone ID: 1, Current visitors: 80"));
        assertTrue(result.contains("Zone ID: 2, Current visitors: 30"));

        // Check the final occupancy after five minutes.
        assertTrue(result.contains("Minute 5:"));
        assertTrue(result.contains("Zone ID: 1, Current visitors: 30"));
        assertTrue(result.contains("Zone ID: 2, Current visitors: 80"));

        // Check that all six minute labels were printed.
        for (int minute = 0; minute <= 5; minute++) {
            assertTrue(
                    result.contains("Minute " + minute + ":"),
                    "Missing output for minute " + minute
            );
        }

        // Run the simulation again and check its returned history.
        List<OccupancyRecord> history =
                engine.runSimpleSimulation(List.of(zoneA, zoneB), connection, 5);

        // Two zones recorded at minutes 0, 1, 2, 3, 4, and 5.
        assertEquals(12, history.size());


        // Verify every zone's occupancy at every simulated minute.
        for (int minute = 0; minute <= 5; minute++) {

            int expectedZoneA = 80 - (minute * 10);
            int expectedZoneB = 30 + (minute * 10);

            assertTrue(
                    history.contains(
                            new OccupancyRecord(minute, 1, expectedZoneA)
                    ),
                    "Incorrect Zone A occupancy at minute " + minute
            );

            assertTrue(
                    history.contains(
                            new OccupancyRecord(minute, 2, expectedZoneB)
                    ),
                    "Incorrect Zone B occupancy at minute " + minute
            );
        }



        // Check the starting occupancy in the returned history.
        assertTrue(history.contains(new OccupancyRecord(0, 1, 80)));
        assertTrue(history.contains(new OccupancyRecord(0, 2, 30)));

        // Check the final occupancy in the returned history.
        assertTrue(history.contains(new OccupancyRecord(5, 1, 30)));
        assertTrue(history.contains(new OccupancyRecord(5, 2, 80)));

        // Check the occupancy after the first minute.
        assertTrue(history.contains(new OccupancyRecord(1, 1, 70)));
        assertTrue(history.contains(new OccupancyRecord(1, 2, 40)));


    }


    /**
     * What we're testing
     * - Zone B starts at 47 out of 50 visitors.
     * - The corridor allows up to 10 visitors per minute.
     * - Only 3 visitors can enter Zone B.
     * - Zone A decreases from 20 to 17.
     * - Zone B reaches exactly 50, never exceeding its capacity.
     * - The total remains 67.
     */

    @Test
    void shouldNotExceedDestinationCapacity() {

        // Zone A has 20 visitors and can hold up to 100.
        Zone zoneA = new Zone();
        zoneA.setId(1);
        zoneA.setName("Zone A");
        zoneA.setCapacity(100);
        zoneA.setInitialVisitors(20);

        // Zone B has room for only 3 more visitors.
        Zone zoneB = new Zone();
        zoneB.setId(2);
        zoneB.setName("Zone B");
        zoneB.setCapacity(50);
        zoneB.setInitialVisitors(47);

        // The corridor allows up to 10 visitors per minute.
        ZoneConnection connection = new ZoneConnection();
        connection.setId(1);
        connection.setFromZone(zoneA);
        connection.setToZone(zoneB);
        connection.setMaxVisitorsPerMinute(10);

        CrowdSimulationEngine engine = new CrowdSimulationEngine();

        Map<Integer, Integer> currentOccupancy =
                engine.initializeOccupancy(List.of(zoneA, zoneB));

        Map<Integer, Integer> nextOccupancy =
                engine.moveVisitorsOneMinute(currentOccupancy, connection, 10);

        // Only 3 visitors can enter Zone B.
        assertEquals(17, nextOccupancy.get(1));
        assertEquals(50, nextOccupancy.get(2));

        // The total number of visitors must remain unchanged.
        assertEquals(67, nextOccupancy.get(1) + nextOccupancy.get(2));
    }



    // We test the queue here
    // We want to make sure the state can represent a full Zone B with 50 visitors and a queue of 7, without mixing the two numbers.
    @Test
    void shouldStoreOccupancyAndQueueLengthsSeparately() {

        // Visitors physically inside each zone.
        Map<Integer, Integer> occupancy = Map.of(
                1, 20,
                2, 50
        );

        // Visitors waiting at each zone's entrance.
        Map<Integer, Integer> queueLengths = Map.of(
                1, 0,
                2, 7
        );

        SimulationState state =
                new SimulationState(occupancy, queueLengths);

        // Check occupancy and queue lengths independently.
        assertEquals(20, state.occupancy().get(1));
        assertEquals(50, state.occupancy().get(2));

        assertEquals(0, state.queueLengths().get(1));
        assertEquals(7, state.queueLengths().get(2));
    }




    // people in queue have a priority over new arrivals
    @Test
    void shouldGiveExistingQueuePriorityOverNewArrivals() {

        // Zone A has 20 visitors and enough room for more.
        Zone zoneA = new Zone();
        zoneA.setId(1);
        zoneA.setName("Zone A");
        zoneA.setCapacity(100);
        zoneA.setInitialVisitors(20);

        // Zone B has only one space left.
        Zone zoneB = new Zone();
        zoneB.setId(2);
        zoneB.setName("Zone B");
        zoneB.setCapacity(50);
        zoneB.setInitialVisitors(49);

        // The corridor allows up to 2 visitors per minute.
        ZoneConnection connection = new ZoneConnection();
        connection.setId(1);
        connection.setFromZone(zoneA);
        connection.setToZone(zoneB);
        connection.setMaxVisitorsPerMinute(2);

        // One visitor is already waiting at Zone B's entrance.
        SimulationState currentState = new SimulationState(
                Map.of(1, 20, 2, 49),
                Map.of(1, 0, 2, 1)
        );

        CrowdSimulationEngine engine = new CrowdSimulationEngine();

        // We will add this method to the engine in our next step.
        SimulationState nextState =
                engine.processConnectionWithQueue(
                        currentState,
                        connection,
                        1
                );

        // The new visitor leaves Zone A and waits in Zone B's queue.
        assertEquals(19, nextState.occupancy().get(1));

        // The existing queued visitor enters Zone B and fills its last space.
        assertEquals(50, nextState.occupancy().get(2));

        // The new visitor joins the queue after the existing visitor gets priority.
        assertEquals(1, nextState.queueLengths().get(2));

        // No new visitor entered Zone B during this minute.
        // The new request remains represented in Zone A for now.
    }



    @Test
    void shouldAddNewVisitorsToQueueWhenDestinationIsFull() {

        // Zone A has 20 visitors who want to move.
        Zone zoneA = new Zone();
        zoneA.setId(1);
        zoneA.setName("Zone A");
        zoneA.setCapacity(100);
        zoneA.setInitialVisitors(20);

        // Zone B is already full, so nobody can enter it yet.
        Zone zoneB = new Zone();
        zoneB.setId(2);
        zoneB.setName("Zone B");
        zoneB.setCapacity(50);
        zoneB.setInitialVisitors(50);

        // The corridor can carry up to 2 visitors per minute.
        ZoneConnection connection = new ZoneConnection();
        connection.setId(1);
        connection.setFromZone(zoneA);
        connection.setToZone(zoneB);
        connection.setMaxVisitorsPerMinute(2);

        // Two visitors are already waiting at Zone B's entrance.
        SimulationState currentState = new SimulationState(
                Map.of(1, 20, 2, 50),
                Map.of(1, 0, 2, 2)
        );

        CrowdSimulationEngine engine = new CrowdSimulationEngine();

        // Two new visitors reach the full destination.
        SimulationState nextState =
                engine.processConnectionWithQueue(
                        currentState,
                        connection,
                        2
                );

        // New visitors leave Zone A to join Zone B's queue.
        assertEquals(18, nextState.occupancy().get(1));

        // Zone B must never exceed its capacity.
        assertEquals(50, nextState.occupancy().get(2));

        // The two existing visitors and two new visitors are waiting.
        assertEquals(4, nextState.queueLengths().get(2));
    }



    @Test
    void shouldRespectCorridorLimitWhenVisitorsJoinQueue() {

        // Zone A has five visitors who want to move.
        Zone zoneA = new Zone();
        zoneA.setId(1);
        zoneA.setName("Zone A");
        zoneA.setCapacity(100);
        zoneA.setInitialVisitors(5);

        // Zone B is full, so new visitors cannot enter yet.
        Zone zoneB = new Zone();
        zoneB.setId(2);
        zoneB.setName("Zone B");
        zoneB.setCapacity(50);
        zoneB.setInitialVisitors(50);

        // The corridor allows only two visitors per minute.
        ZoneConnection connection = new ZoneConnection();
        connection.setId(1);
        connection.setFromZone(zoneA);
        connection.setToZone(zoneB);
        connection.setMaxVisitorsPerMinute(2);

        // There is nobody in the queue at the start.
        SimulationState currentState = new SimulationState(
                Map.of(1, 5, 2, 50),
                Map.of(1, 0, 2, 0)
        );

        CrowdSimulationEngine engine = new CrowdSimulationEngine();

        // Five visitors request movement, but the corridor limit is two.
        SimulationState nextState =
                engine.processConnectionWithQueue(
                        currentState,
                        connection,
                        5
                );

        // Only two visitors can leave Zone A this minute.
        assertEquals(3, nextState.occupancy().get(1));

        // Zone B remains full.
        assertEquals(50, nextState.occupancy().get(2));

        // Only the two visitors allowed through the corridor join the queue.
        assertEquals(2, nextState.queueLengths().get(2));
    }


    // if there is available space in zone B and there is a queue
    @Test
    void shouldMoveWaitingVisitorsIntoDestinationWhenSpaceOpens() {

        // Zone A has five visitors who could request movement.
        Zone zoneA = new Zone();
        zoneA.setId(1);
        zoneA.setName("Zone A");
        zoneA.setCapacity(100);
        zoneA.setInitialVisitors(5);

        // Zone B has two available spaces.
        Zone zoneB = new Zone();
        zoneB.setId(2);
        zoneB.setName("Zone B");
        zoneB.setCapacity(50);
        zoneB.setInitialVisitors(48);

        // The corridor allows up to two visitors per minute.
        ZoneConnection connection = new ZoneConnection();
        connection.setId(1);
        connection.setFromZone(zoneA);
        connection.setToZone(zoneB);
        connection.setMaxVisitorsPerMinute(2);

        // Two visitors are already waiting at Zone B's entrance.
        SimulationState currentState = new SimulationState(
                Map.of(1, 5, 2, 48),
                Map.of(1, 0, 2, 2)
        );

        CrowdSimulationEngine engine = new CrowdSimulationEngine();

        // No new visitors request movement during this minute.
        SimulationState nextState =
                engine.processConnectionWithQueue(
                        currentState,
                        connection,
                        0
                );

        // No new visitors leave Zone A.
        assertEquals(5, nextState.occupancy().get(1));

        // The two waiting visitors enter Zone B.
        assertEquals(50, nextState.occupancy().get(2));

        // Both visitors have left the queue.
        assertEquals(0, nextState.queueLengths().get(2));
    }



    @Test
    void shouldRecordOccupancyAndQueueLengthForEveryMinute() {

        // Zone A starts with 20 visitors.
        Zone zoneA = new Zone();
        zoneA.setId(1);
        zoneA.setName("Zone A");
        zoneA.setCapacity(100);
        zoneA.setInitialVisitors(20);

        // Zone B starts with 30 visitors.
        Zone zoneB = new Zone();
        zoneB.setId(2);
        zoneB.setName("Zone B");
        zoneB.setCapacity(150);
        zoneB.setInitialVisitors(30);

        // Visitors can move from Zone A to Zone B.
        ZoneConnection connection = new ZoneConnection();
        connection.setId(1);
        connection.setFromZone(zoneA);
        connection.setToZone(zoneB);
        connection.setMaxVisitorsPerMinute(10);

        CrowdSimulationEngine engine = new CrowdSimulationEngine();

        // Run for five minutes and collect complete snapshots.
        List<SimulationSnapshotRecord> history =
                engine.runSimulationWithSnapshots(
                        List.of(zoneA, zoneB),
                        connection,
                        5
                );

        // Two zones are recorded at minutes zero through five.
        assertEquals(12, history.size());

        // Minute zero records the initial occupancy and empty queues.
        assertTrue(history.contains(
                new SimulationSnapshotRecord(0, 1, 20, 0)
        ));
        assertTrue(history.contains(
                new SimulationSnapshotRecord(0, 2, 30, 0)
        ));

        // Minute five records the final occupancy and queue lengths.
        assertTrue(history.contains(
                new SimulationSnapshotRecord(5, 1, 0, 0)
        ));
        assertTrue(history.contains(
                new SimulationSnapshotRecord(5, 2, 50, 0)
        ));
    }



    @Test
    void shouldSupportMultipleConnectionsFromOneSource() {
        // Create the entrance and two destination zones.
        Zone zoneA = new Zone();
        zoneA.setId(1);
        zoneA.setName("Entrance");
        zoneA.setCapacity(100);
        zoneA.setInitialVisitors(20);

        Zone zoneB = new Zone();
        zoneB.setId(2);
        zoneB.setName("Exhibition Hall");
        zoneB.setCapacity(100);
        zoneB.setInitialVisitors(0);

        Zone zoneC = new Zone();
        zoneC.setId(3);
        zoneC.setName("Food Court");
        zoneC.setCapacity(100);
        zoneC.setInitialVisitors(0);

        // Connect the entrance to both destination zones.
        ZoneConnection connectionAB =
                new ZoneConnection(1, 3, zoneA, zoneB);

        ZoneConnection connectionAC =
                new ZoneConnection(2, 2, zoneA, zoneC);

        // Run the simulation for one minute.
        List<SimulationSnapshotRecord> history =
                new CrowdSimulationEngine().runSimulationWithMultipleConnections(
                        List.of(zoneA, zoneB, zoneC),
                        List.of(connectionAB, connectionAC),
                        1
                );

        // Verify the expected occupancy after minute one.
        assertTrue(history.contains(
                new SimulationSnapshotRecord(1, 1, 15, 0)));

        assertTrue(history.contains(
                new SimulationSnapshotRecord(1, 2, 3, 0)));

        assertTrue(history.contains(
                new SimulationSnapshotRecord(1, 3, 2, 0)));
    }



    @Test
    void shouldRespectCapacityWhenMultipleConnectionsShareDestination() {
        // Create three zones: two sources and one shared destination.
        Zone zoneA = new Zone();
        zoneA.setId(1);
        zoneA.setName("Entrance A");
        zoneA.setCapacity(100);
        zoneA.setInitialVisitors(10);

        Zone zoneB = new Zone();
        zoneB.setId(2);
        zoneB.setName("Shared Hall");
        zoneB.setCapacity(12);
        zoneB.setInitialVisitors(10);

        Zone zoneC = new Zone();
        zoneC.setId(3);
        zoneC.setName("Entrance C");
        zoneC.setCapacity(100);
        zoneC.setInitialVisitors(10);

        // Both corridors lead to the same destination.
        ZoneConnection connectionAB =
                new ZoneConnection(1, 3, zoneA, zoneB);

        ZoneConnection connectionCB =
                new ZoneConnection(2, 3, zoneC, zoneB);

        // Run for one minute with both corridors active.
        List<SimulationSnapshotRecord> history =
                new CrowdSimulationEngine().runSimulationWithMultipleConnections(
                        List.of(zoneA, zoneB, zoneC),
                        List.of(connectionAB, connectionCB),
                        1
                );
        // Debug: show the snapshots produced by the engine.
        history.forEach(System.out::println);


        // The shared destination must never exceed its capacity.
        assertTrue(history.contains(
                new SimulationSnapshotRecord(1, 2, 12, 4)));

        // Each source sends three visitors toward the destination.
        assertTrue(history.contains(
                new SimulationSnapshotRecord(1, 1, 7, 0)));

        assertTrue(history.contains(
                new SimulationSnapshotRecord(1, 3, 7, 0)));


    }




    @Test
    void shouldRespectCorridorThroughputWhenQueueExists() {
        // Create a source zone and a destination with limited space.
        Zone zoneA = new Zone();
        zoneA.setId(1);
        zoneA.setName("Entrance");
        zoneA.setCapacity(100);
        zoneA.setInitialVisitors(10);

        Zone zoneB = new Zone();
        zoneB.setId(2);
        zoneB.setName("Exhibition Hall");
        zoneB.setCapacity(20);
        zoneB.setInitialVisitors(10);

        // This corridor can carry at most three visitors per minute.
        ZoneConnection connectionAB =
                new ZoneConnection(1, 3, zoneA, zoneB);

        // Start the simulation with two visitors already waiting at B.
        SimulationState initialState = new SimulationState(
                Map.of(1, 10, 2, 10),
                Map.of(1, 0, 2, 2)
        );

        CrowdSimulationEngine engine = new CrowdSimulationEngine();

        // Verify the queue-aware movement rule directly.
        SimulationState nextState =
                engine.processConnectionWithQueue(initialState, connectionAB, 10);

        // Two queued visitors enter first, leaving capacity for one new visitor.
        assertEquals(9, nextState.occupancy().get(1));
        assertEquals(13, nextState.occupancy().get(2));
        assertEquals(0, nextState.queueLengths().get(2));
    }



    @Test
    void shouldRespectCorridorThroughputWhenMultipleConnectionsShareDestination() {
        // Create two source zones and one shared destination.
        Zone zoneA = new Zone();
        zoneA.setId(1);
        zoneA.setName("Entrance A");
        zoneA.setCapacity(100);
        zoneA.setInitialVisitors(10);

        Zone zoneB = new Zone();
        zoneB.setId(2);
        zoneB.setName("Shared Hall");
        zoneB.setCapacity(20);
        zoneB.setInitialVisitors(10);

        Zone zoneC = new Zone();
        zoneC.setId(3);
        zoneC.setName("Entrance C");
        zoneC.setCapacity(100);
        zoneC.setInitialVisitors(10);

        // Both corridors can carry at most three visitors per minute.
        ZoneConnection connectionAB =
                new ZoneConnection(1, 3, zoneA, zoneB);

        ZoneConnection connectionCB =
                new ZoneConnection(2, 3, zoneC, zoneB);

        // Start with two visitors already waiting at the shared destination.
        SimulationState initialState = new SimulationState(
                Map.of(1, 10, 2, 10, 3, 10),
                Map.of(1, 0, 2, 2, 3, 0)
        );

        List<SimulationSnapshotRecord> history =
                new CrowdSimulationEngine().runSimulationWithMultipleConnections(
                        List.of(zoneA, zoneB, zoneC),
                        List.of(connectionAB, connectionCB),
                        1
                );

        // The shared destination must never exceed its capacity.
        SimulationSnapshotRecord destinationSnapshot =
                history.stream()
                        .filter(snapshot ->
                                snapshot.minute() == 1 && snapshot.zoneId() == 2)
                        .findFirst()
                        .orElseThrow();

        assertTrue(destinationSnapshot.occupancy() <= zoneB.getCapacity());

        // Each source must retain a non-negative occupancy.
        SimulationSnapshotRecord sourceASnapshot =
                history.stream()
                        .filter(snapshot ->
                                snapshot.minute() == 1 && snapshot.zoneId() == 1)
                        .findFirst()
                        .orElseThrow();

        SimulationSnapshotRecord sourceCSnapshot =
                history.stream()
                        .filter(snapshot ->
                                snapshot.minute() == 1 && snapshot.zoneId() == 3)
                        .findFirst()
                        .orElseThrow();

        assertTrue(sourceASnapshot.occupancy() >= 0);
        assertTrue(sourceCSnapshot.occupancy() >= 0);
    }



    @Test
    void shouldPreserveExistingQueueInInitialSnapshot() {
        // Arrange: create two connected zones.
        Zone zoneA = new Zone();
        zoneA.setId(1);
        zoneA.setName("Zone A");
        zoneA.setCapacity(20);
        zoneA.setInitialVisitors(10);

        Zone zoneB = new Zone();
        zoneB.setId(2);
        zoneB.setName("Zone B");
        zoneB.setCapacity(20);
        zoneB.setInitialVisitors(5);

        ZoneConnection connection = new ZoneConnection();
        connection.setMaxVisitorsPerMinute(3);
        connection.setFromZone(zoneA);
        connection.setToZone(zoneB);

        Map<Integer, Integer> occupancy = new HashMap<>();
        occupancy.put(1, 10);
        occupancy.put(2, 5);

        Map<Integer, Integer> queues = new HashMap<>();
        queues.put(1, 0);
        queues.put(2, 2);

        SimulationState initialState =
                new SimulationState(occupancy, queues);

        CrowdSimulationEngine engine = new CrowdSimulationEngine();

        // Act: run for zero minutes to inspect the initial snapshot.
        List<SimulationSnapshotRecord> history =
                engine.runSimulationWithMultipleConnections(
                        List.of(zoneA, zoneB),
                        List.of(connection),
                        0,
                        initialState
                );

        // Assert: the initial queue must be preserved.
        assertEquals(2, history.size());
        assertEquals(2, history.stream()
                .filter(snapshot -> snapshot.zoneId() == 2)
                .findFirst()
                .orElseThrow()
                .queueLength());
    }



    @Test
    void shouldNotCountTheSameQueueTwiceForMultipleIncomingCorridors() {
        Zone zoneA = new Zone();
        zoneA.setId(1);
        zoneA.setName("Zone A");
        zoneA.setCapacity(20);
        zoneA.setInitialVisitors(10);

        Zone zoneB = new Zone();
        zoneB.setId(2);
        zoneB.setName("Zone B");
        zoneB.setCapacity(20);
        zoneB.setInitialVisitors(10);

        Zone zoneC = new Zone();
        zoneC.setId(3);
        zoneC.setName("Zone C");
        zoneC.setCapacity(20);
        zoneC.setInitialVisitors(0);

        ZoneConnection connectionAtoC = new ZoneConnection();
        connectionAtoC.setMaxVisitorsPerMinute(3);
        connectionAtoC.setFromZone(zoneA);
        connectionAtoC.setToZone(zoneC);

        ZoneConnection connectionBtoC = new ZoneConnection();
        connectionBtoC.setMaxVisitorsPerMinute(3);
        connectionBtoC.setFromZone(zoneB);
        connectionBtoC.setToZone(zoneC);

        Map<Integer, Integer> occupancy = new HashMap<>();
        occupancy.put(1, 10);
        occupancy.put(2, 10);
        occupancy.put(3, 0);

        Map<Integer, Integer> queues = new HashMap<>();
        queues.put(1, 0);
        queues.put(2, 0);
        queues.put(3, 2);

        SimulationState initialState =
                new SimulationState(occupancy, queues);

        CrowdSimulationEngine engine = new CrowdSimulationEngine();

        List<SimulationSnapshotRecord> history =
                engine.runSimulationWithMultipleConnections(
                        List.of(zoneA, zoneB, zoneC),
                        List.of(connectionAtoC, connectionBtoC),
                        1,
                        initialState
                );

        int destinationOccupancy = history.stream()
                .filter(snapshot ->
                        snapshot.minute() == 1 && snapshot.zoneId() == 3)
                .findFirst()
                .orElseThrow()
                .occupancy();

        int destinationQueue = history.stream()
                .filter(snapshot ->
                        snapshot.minute() == 1 && snapshot.zoneId() == 3)
                .findFirst()
                .orElseThrow()
                .queueLength();

        assertEquals(6, destinationOccupancy);
        assertEquals(0, destinationQueue);

    }



    @Test
    void shouldPreserveTotalVisitorsAcrossMultipleCorridors() {
        // Arrange: create three zones.
        Zone zoneA = new Zone();
        zoneA.setId(1);
        zoneA.setName("Zone A");
        zoneA.setCapacity(20);
        zoneA.setInitialVisitors(10);

        Zone zoneB = new Zone();
        zoneB.setId(2);
        zoneB.setName("Zone B");
        zoneB.setCapacity(20);
        zoneB.setInitialVisitors(10);

        Zone zoneC = new Zone();
        zoneC.setId(3);
        zoneC.setName("Zone C");
        zoneC.setCapacity(20);
        zoneC.setInitialVisitors(0);

        ZoneConnection connectionAtoC = new ZoneConnection();
        connectionAtoC.setMaxVisitorsPerMinute(3);
        connectionAtoC.setFromZone(zoneA);
        connectionAtoC.setToZone(zoneC);

        ZoneConnection connectionBtoC = new ZoneConnection();
        connectionBtoC.setMaxVisitorsPerMinute(3);
        connectionBtoC.setFromZone(zoneB);
        connectionBtoC.setToZone(zoneC);

        Map<Integer, Integer> occupancy = new HashMap<>();
        occupancy.put(1, 10);
        occupancy.put(2, 10);
        occupancy.put(3, 0);

        Map<Integer, Integer> queues = new HashMap<>();
        queues.put(1, 0);
        queues.put(2, 0);
        queues.put(3, 2);

        SimulationState initialState =
                new SimulationState(occupancy, queues);

        CrowdSimulationEngine engine = new CrowdSimulationEngine();

        // Act: run the simulation for five minutes.
        List<SimulationSnapshotRecord> history =
                engine.runSimulationWithMultipleConnections(
                        List.of(zoneA, zoneB, zoneC),
                        List.of(connectionAtoC, connectionBtoC),
                        5,
                        initialState
                );

        // Assert: count visitors inside zones and waiting in queues.
        int initialTotal = occupancy.values().stream()
                .mapToInt(Integer::intValue)
                .sum()
                + queues.values().stream()
                .mapToInt(Integer::intValue)
                .sum();


        for (int minute = 0; minute <= 5; minute++) {
            final int currentMinute = minute;

            int occupancyTotal = history.stream()
                    .filter(snapshot -> snapshot.minute() == currentMinute)
                    .mapToInt(SimulationSnapshotRecord::occupancy)
                    .sum();

            int queueTotal = history.stream()
                    .filter(snapshot -> snapshot.minute() == currentMinute)
                    .mapToInt(SimulationSnapshotRecord::queueLength)
                    .sum();

            assertEquals(
                    initialTotal,
                    occupancyTotal + queueTotal,
                    "Visitor total changed at minute " + currentMinute
            );
        }

    }




    @Test
    void shouldNeverExceedZoneCapacityAcrossMultipleMinutes() {
        Zone zoneA = new Zone();
        zoneA.setId(1);
        zoneA.setName("Zone A");
        zoneA.setCapacity(20);
        zoneA.setInitialVisitors(10);

        Zone zoneB = new Zone();
        zoneB.setId(2);
        zoneB.setName("Zone B");
        zoneB.setCapacity(12);
        zoneB.setInitialVisitors(10);

        Zone zoneC = new Zone();
        zoneC.setId(3);
        zoneC.setName("Zone C");
        zoneC.setCapacity(20);
        zoneC.setInitialVisitors(10);

        ZoneConnection connectionAtoB = new ZoneConnection();
        connectionAtoB.setMaxVisitorsPerMinute(5);
        connectionAtoB.setFromZone(zoneA);
        connectionAtoB.setToZone(zoneB);

        ZoneConnection connectionCtoB = new ZoneConnection();
        connectionCtoB.setMaxVisitorsPerMinute(5);
        connectionCtoB.setFromZone(zoneC);
        connectionCtoB.setToZone(zoneB);

        CrowdSimulationEngine engine = new CrowdSimulationEngine();

        List<SimulationSnapshotRecord> history =
                engine.runSimulationWithMultipleConnections(
                        List.of(zoneA, zoneB, zoneC),
                        List.of(connectionAtoB, connectionCtoB),
                        5
                );

        for (SimulationSnapshotRecord snapshot : history) {
            int capacity;

            if (snapshot.zoneId() == 1) {
                capacity = 20;
            } else if (snapshot.zoneId() == 2) {
                capacity = 12;
            } else if (snapshot.zoneId() == 3) {
                capacity = 20;
            } else {
                throw new AssertionError(
                        "Unexpected zone ID: " + snapshot.zoneId()
                );
            }

            assertTrue(
                    snapshot.occupancy() <= capacity,
                    "Zone " + snapshot.zoneId()
                            + " exceeded capacity at minute "
                            + snapshot.minute()
            );

            assertTrue(
                    snapshot.occupancy() >= 0,
                    "Zone occupancy became negative at minute "
                            + snapshot.minute()
            );
        }
    }



    @Test
    void shouldRespectCorridorThroughputWhenQueueExistsAcrossMultipleMinutes() {
        Zone zoneA = new Zone();
        zoneA.setId(1);
        zoneA.setName("Zone A");
        zoneA.setCapacity(20);
        zoneA.setInitialVisitors(10);

        Zone zoneB = new Zone();
        zoneB.setId(2);
        zoneB.setName("Zone B");
        zoneB.setCapacity(20);
        zoneB.setInitialVisitors(10);

        ZoneConnection connection = new ZoneConnection();
        connection.setMaxVisitorsPerMinute(3);
        connection.setFromZone(zoneA);
        connection.setToZone(zoneB);

        Map<Integer, Integer> occupancy = new HashMap<>();
        occupancy.put(1, 10);
        occupancy.put(2, 10);

        Map<Integer, Integer> queues = new HashMap<>();
        queues.put(1, 0);
        queues.put(2, 2);

        SimulationState initialState =
                new SimulationState(occupancy, queues);

        CrowdSimulationEngine engine = new CrowdSimulationEngine();

        List<SimulationSnapshotRecord> history =
                engine.runSimulationWithMultipleConnections(
                        List.of(zoneA, zoneB),
                        List.of(connection),
                        3,
                        initialState
                );

        for (int minute = 1; minute <= 3; minute++) {
            final int currentMinute = minute;

            int destinationOccupancy = history.stream()
                    .filter(snapshot ->
                            snapshot.minute() == currentMinute
                                    && snapshot.zoneId() == 2)
                    .findFirst()
                    .orElseThrow()
                    .occupancy();

            int previousOccupancy = history.stream()
                    .filter(snapshot ->
                            snapshot.minute() == currentMinute - 1
                                    && snapshot.zoneId() == 2)
                    .findFirst()
                    .orElseThrow()
                    .occupancy();

            int visitorsEntered =
                    destinationOccupancy - previousOccupancy;

            System.out.println(
                    "Minute " + currentMinute
                            + ": previous occupancy = " + previousOccupancy
                            + ", current occupancy = " + destinationOccupancy
                            + ", occupancy change = " + visitorsEntered
            );

            assertTrue(
                    visitorsEntered <= 3,
                    "Corridor throughput exceeded at minute "
                            + currentMinute
            );
        }
    }











} // class end
