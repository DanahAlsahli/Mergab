
package com.example.mergab.Simulation;

import com.example.mergab.Model.ExpoEvent;
import com.example.mergab.Model.Zone;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

class EventDrivenSimulationRunnerTest {

    @Test
    void shouldAddScheduledEventArrivalsToCorrectZone() {
        Zone zone = new Zone();
        zone.setId(1);
        zone.setName("Exhibition Hall");
        zone.setCapacity(100);
        zone.setInitialVisitors(0);

        LocalDateTime simulationStart =
                LocalDateTime.of(2026, 10, 16, 10, 0);

        ExpoEvent event = new ExpoEvent();
        event.setId(1);
        event.setName("Design Exhibition");
        event.setZone(zone);
        event.setExpectedDemand(10);
        event.setStartDateTime(
                LocalDateTime.of(2026, 10, 16, 10, 10)
        );
        event.setEndDateTime(
                LocalDateTime.of(2026, 10, 16, 11, 0)
        );

        EventDrivenSimulationRunner runner =
                new EventDrivenSimulationRunner(
                        new CrowdSimulationEngine(),
                        new EventDrivenSimulationEngine()
                );

        List<SimulationSnapshotRecord> snapshots =
                runner.runSimulation(
                        List.of(zone),
                        List.of(),
                        List.of(event),
                        simulationStart,
                        15,
                        5
                );

        SimulationSnapshotRecord minuteZero = snapshots.stream()
                .filter(s -> s.minute() == 0 && s.zoneId() == 1)
                .findFirst()
                .orElseThrow();

        SimulationSnapshotRecord minuteTen = snapshots.stream()
                .filter(s -> s.minute() == 10 && s.zoneId() == 1)
                .findFirst()
                .orElseThrow();

        assertEquals(0, minuteZero.occupancy());
        assertEquals(10, minuteTen.occupancy());
    }

    @Test
    void shouldNeverExceedZoneCapacityWhenVisitorsArrive() {
        Zone zone = new Zone();
        zone.setId(1);
        zone.setName("Small Hall");
        zone.setCapacity(5);
        zone.setInitialVisitors(0);

        LocalDateTime start =
                LocalDateTime.of(2026, 10, 16, 10, 0);

        ExpoEvent event = new ExpoEvent();
        event.setId(1);
        event.setName("Popular Event");
        event.setZone(zone);
        event.setExpectedDemand(10);
        event.setStartDateTime(start.plusMinutes(5));
        event.setEndDateTime(start.plusMinutes(30));

        EventDrivenSimulationRunner runner =
                new EventDrivenSimulationRunner(
                        new CrowdSimulationEngine(),
                        new EventDrivenSimulationEngine()
                );

        List<SimulationSnapshotRecord> snapshots =
                runner.runSimulation(
                        List.of(zone),
                        List.of(),
                        List.of(event),
                        start,
                        10,
                        5
                );

        assertTrue(snapshots.stream().allMatch(
                s -> s.occupancy() <= zone.getCapacity()
        ));

        SimulationSnapshotRecord finalSnapshot = snapshots.stream()
                .filter(s -> s.minute() == 10 && s.zoneId() == 1)
                .findFirst()
                .orElseThrow();

        assertEquals(5, finalSnapshot.occupancy());
        assertEquals(5, finalSnapshot.queueLength());
    }


    @Test
    void shouldApplyScheduledArrivalsOnlyOnce() {
        Zone zone = new Zone();
        zone.setId(1);
        zone.setName("Exhibition Hall");
        zone.setCapacity(100);
        zone.setInitialVisitors(0);

        LocalDateTime start = LocalDateTime.of(2026, 10, 16, 10, 0);

        ExpoEvent event = new ExpoEvent();
        event.setId(1);
        event.setName("Design Exhibition");
        event.setZone(zone);
        event.setExpectedDemand(10);
        event.setStartDateTime(start.plusMinutes(10));
        event.setEndDateTime(start.plusMinutes(60));

        EventDrivenSimulationRunner runner =
                new EventDrivenSimulationRunner(
                        new CrowdSimulationEngine(),
                        new EventDrivenSimulationEngine()
                );

        List<SimulationSnapshotRecord> snapshots =
                runner.runSimulation(
                        List.of(zone),
                        List.of(),
                        List.of(event),
                        start,
                        15,
                        5
                );

        SimulationSnapshotRecord finalSnapshot = snapshots.stream()
                .filter(s -> s.minute() == 15 && s.zoneId() == 1)
                .findFirst()
                .orElseThrow();

        assertEquals(10, finalSnapshot.occupancy());
        assertEquals(0, finalSnapshot.queueLength());
    }

    @Test
    void shouldCombineDemandFromMultipleEventsInSameZone() {
        Zone zone = new Zone();
        zone.setId(1);
        zone.setName("Exhibition Hall");
        zone.setCapacity(100);
        zone.setInitialVisitors(0);

        LocalDateTime start = LocalDateTime.of(2026, 10, 16, 10, 0);

        ExpoEvent firstEvent = new ExpoEvent();
        firstEvent.setId(1);
        firstEvent.setName("Design Exhibition");
        firstEvent.setZone(zone);
        firstEvent.setExpectedDemand(10);
        firstEvent.setStartDateTime(start.plusMinutes(10));
        firstEvent.setEndDateTime(start.plusMinutes(30));

        ExpoEvent secondEvent = new ExpoEvent();
        secondEvent.setId(2);
        secondEvent.setName("Technology Exhibition");
        secondEvent.setZone(zone);
        secondEvent.setExpectedDemand(5);
        secondEvent.setStartDateTime(start.plusMinutes(10));
        secondEvent.setEndDateTime(start.plusMinutes(30));

        EventDrivenSimulationRunner runner =
                new EventDrivenSimulationRunner(
                        new CrowdSimulationEngine(),
                        new EventDrivenSimulationEngine()
                );

        List<SimulationSnapshotRecord> snapshots =
                runner.runSimulation(
                        List.of(zone),
                        List.of(),
                        List.of(firstEvent, secondEvent),
                        start,
                        15,
                        5
                );

        SimulationSnapshotRecord finalSnapshot = snapshots.stream()
                .filter(s -> s.minute() == 15 && s.zoneId() == 1)
                .findFirst()
                .orElseThrow();

        assertEquals(15, finalSnapshot.occupancy());
        assertEquals(0, finalSnapshot.queueLength());
    }


    @Test
    void shouldContinueFromExistingSimulationState() {
        Zone zone = new Zone();
        zone.setId(1);
        zone.setName("Main Hall");
        zone.setCapacity(100);
        zone.setInitialVisitors(10);

        SimulationState existingState = new SimulationState(
                Map.of(1, 35),
                Map.of(1, 7)
        );

        EventDrivenSimulationRunner runner =
                new EventDrivenSimulationRunner(
                        new CrowdSimulationEngine(),
                        new EventDrivenSimulationEngine()
                );

        List<SimulationSnapshotRecord> snapshots =
                runner.runSimulation(
                        List.of(zone),
                        List.of(),
                        List.of(),
                        LocalDateTime.of(2026, 10, 15, 10, 0),
                        2,
                        30,
                        existingState
                );

        SimulationSnapshotRecord minuteZero = snapshots.stream()
                .filter(s -> s.minute() == 0 && s.zoneId() == 1)
                .findFirst()
                .orElseThrow();

        assertEquals(35, minuteZero.occupancy());
        assertEquals(7, minuteZero.queueLength());
    }



    @Test
    void shouldPreserveOccupancyAcrossConsecutiveSegments() {
        Zone zone = new Zone();
        zone.setId(1);
        zone.setName("Main Hall");
        zone.setCapacity(100);
        zone.setInitialVisitors(10);

        EventDrivenSimulationRunner runner =
                new EventDrivenSimulationRunner(
                        new CrowdSimulationEngine(),
                        new EventDrivenSimulationEngine()
                );

        LocalDateTime firstStart =
                LocalDateTime.of(2026, 10, 15, 10, 0);

        List<SimulationSnapshotRecord> firstRun =
                runner.runSimulation(
                        List.of(zone),
                        List.of(),
                        List.of(),
                        firstStart,
                        2,
                        30
                );

        SimulationSnapshotRecord lastSnapshot = firstRun.stream()
                .filter(s -> s.minute() == 2 && s.zoneId() == 1)
                .findFirst()
                .orElseThrow();

        SimulationState stateForNextRun = new SimulationState(
                Map.of(1, lastSnapshot.occupancy()),
                Map.of(1, lastSnapshot.queueLength())
        );

        List<SimulationSnapshotRecord> secondRun =
                runner.runSimulation(
                        List.of(zone),
                        List.of(),
                        List.of(),
                        firstStart.plusMinutes(2),
                        2,
                        30,
                        stateForNextRun
                );

        SimulationSnapshotRecord secondRunStart = secondRun.stream()
                .filter(s -> s.minute() == 0 && s.zoneId() == 1)
                .findFirst()
                .orElseThrow();

        assertEquals(
                lastSnapshot.occupancy(),
                secondRunStart.occupancy()
        );

        assertEquals(
                lastSnapshot.queueLength(),
                secondRunStart.queueLength()
        );
    }






    @Test
    void shouldMoveEventArrivalsFromEntranceTowardEventAcrossMinutes() {
        Zone entrance = new Zone();
        entrance.setId(1);
        entrance.setName("Entrance");
        entrance.setCapacity(100);
        entrance.setInitialVisitors(0);
        entrance.setFacilities(new java.util.HashSet<>());

        Zone corridor = new Zone();
        corridor.setId(2);
        corridor.setName("Corridor");
        corridor.setCapacity(100);
        corridor.setInitialVisitors(0);
        corridor.setFacilities(new java.util.HashSet<>());

        Zone eventZone = new Zone();
        eventZone.setId(3);
        eventZone.setName("Event Hall");
        eventZone.setCapacity(100);
        eventZone.setInitialVisitors(0);
        eventZone.setFacilities(new java.util.HashSet<>());

        com.example.mergab.Model.Facility entranceFacility =
                new com.example.mergab.Model.Facility();
        entranceFacility.setType("ENTRANCE");
        entranceFacility.setZone(entrance);
        entrance.getFacilities().add(entranceFacility);

        com.example.mergab.Model.ZoneConnection first =
                new com.example.mergab.Model.ZoneConnection();
        first.setId(1);
        first.setFromZone(entrance);
        first.setToZone(corridor);
        first.setMaxVisitorsPerMinute(10);

        com.example.mergab.Model.ZoneConnection second =
                new com.example.mergab.Model.ZoneConnection();
        second.setId(2);
        second.setFromZone(corridor);
        second.setToZone(eventZone);
        second.setMaxVisitorsPerMinute(10);

        LocalDateTime start = LocalDateTime.of(2026, 10, 16, 10, 0);
        ExpoEvent event = new ExpoEvent();
        event.setId(1);
        event.setName("Route Test Event");
        event.setZone(eventZone);
        event.setExpectedDemand(10);
        event.setStartDateTime(start.plusMinutes(5));
        event.setEndDateTime(start.plusMinutes(30));

        EventDrivenSimulationRunner runner = new EventDrivenSimulationRunner(
                new CrowdSimulationEngine(),
                new EventDrivenSimulationEngine()
        );

        List<SimulationSnapshotRecord> snapshots = runner.runSimulation(
                List.of(entrance, corridor, eventZone),
                List.of(first, second),
                List.of(event),
                start,
                5,
                5
        );

        int finalEventOccupancy = snapshots.stream()
                .filter(snapshot -> snapshot.minute() == 5
                        && snapshot.zoneId() == 3)
                .findFirst()
                .orElseThrow()
                .occupancy();

        int finalEntranceOccupancy = snapshots.stream()
                .filter(snapshot -> snapshot.minute() == 5
                        && snapshot.zoneId() == 1)
                .findFirst()
                .orElseThrow()
                .occupancy();

        assertTrue(finalEventOccupancy > 0,
                "Some event arrivals should reach the event zone");
        assertTrue(snapshots.stream().allMatch(snapshot ->
                        snapshot.occupancy() <= 100),
                "No zone may exceed its capacity");
        assertTrue(finalEntranceOccupancy < 10,
                "Visitors should not all remain at the entrance");
    }


    @Test
    void routeVisitorsShouldAdvanceAtMostOneConnectionPerMinute() {
        Zone entrance = routeZone(1, "Entrance", 10, 0);
        Zone corridor = routeZone(2, "Corridor", 10, 0);
        Zone eventZone = routeZone(3, "Event Hall", 10, 0);

        com.example.mergab.Model.Facility entranceFacility =
                new com.example.mergab.Model.Facility();
        entranceFacility.setType("ENTRANCE");
        entranceFacility.setZone(entrance);
        entrance.setFacilities(new java.util.HashSet<>(
                java.util.Set.of(entranceFacility)));

        com.example.mergab.Model.ZoneConnection first =
                routeConnection(1, entrance, corridor, 10);
        com.example.mergab.Model.ZoneConnection second =
                routeConnection(2, corridor, eventZone, 10);

        LocalDateTime start = LocalDateTime.of(2026, 10, 16, 10, 0);
        ExpoEvent event = new ExpoEvent();
        event.setId(1);
        event.setName("Route Timing Test");
        event.setZone(eventZone);
        event.setExpectedDemand(1);
        event.setStartDateTime(start.plusMinutes(2));
        event.setEndDateTime(start.plusMinutes(20));

        EventDrivenSimulationRunner runner = new EventDrivenSimulationRunner(
                new CrowdSimulationEngine(), new EventDrivenSimulationEngine());

        List<SimulationSnapshotRecord> snapshots = runner.runSimulation(
                List.of(entrance, corridor, eventZone),
                List.of(first, second), List.of(event), start, 4, 1);

        assertEquals(1, snapshotAt(snapshots, 2, 1).occupancy(),
                "The scheduled visitor should first enter through the entrance");
        assertEquals(0, snapshotAt(snapshots, 2, 2).occupancy(),
                "The visitor must not skip directly into the corridor");
        assertEquals(1, snapshotAt(snapshots, 3, 2).occupancy(),
                "The visitor should advance one connection in the next minute");
        assertEquals(0, snapshotAt(snapshots, 3, 3).occupancy(),
                "The visitor must not cross two connections in one minute");
        assertEquals(1, snapshotAt(snapshots, 4, 3).occupancy(),
                "The visitor should reach the event zone on the following minute");
    }

    @Test
    void routeVisitorsShouldRemainAccountedForWhenDestinationIsFull() {
        Zone entrance = routeZone(1, "Entrance", 10, 0);
        Zone corridor = routeZone(2, "Corridor", 10, 0);
        Zone eventZone = routeZone(3, "Full Event Hall", 1, 1);

        com.example.mergab.Model.Facility entranceFacility =
                new com.example.mergab.Model.Facility();
        entranceFacility.setType("ENTRANCE");
        entranceFacility.setZone(entrance);
        entrance.setFacilities(new java.util.HashSet<>(
                java.util.Set.of(entranceFacility)));

        com.example.mergab.Model.ZoneConnection first =
                routeConnection(1, entrance, corridor, 10);
        com.example.mergab.Model.ZoneConnection second =
                routeConnection(2, corridor, eventZone, 10);

        LocalDateTime start = LocalDateTime.of(2026, 10, 16, 10, 0);
        ExpoEvent event = new ExpoEvent();
        event.setId(1);
        event.setName("Full Hall Test");
        event.setZone(eventZone);
        event.setExpectedDemand(1);
        event.setStartDateTime(start.plusMinutes(2));
        event.setEndDateTime(start.plusMinutes(20));

        EventDrivenSimulationRunner runner = new EventDrivenSimulationRunner(
                new CrowdSimulationEngine(), new EventDrivenSimulationEngine());

        List<SimulationSnapshotRecord> snapshots = runner.runSimulation(
                List.of(entrance, corridor, eventZone),
                List.of(first, second), List.of(event), start, 4, 1);

        SimulationSnapshotRecord finalEntrance = snapshotAt(snapshots, 4, 1);
        SimulationSnapshotRecord finalCorridor = snapshotAt(snapshots, 4, 2);
        SimulationSnapshotRecord finalEvent = snapshotAt(snapshots, 4, 3);

        assertEquals(0, finalEntrance.occupancy());
        assertEquals(1, finalCorridor.occupancy(),
                "The visitor should wait in the last reachable zone when the event zone is full");
        assertEquals(1, finalEvent.occupancy(),
                "The event zone must remain at its capacity");
        assertEquals(2, finalEntrance.occupancy()
                        + finalCorridor.occupancy()
                        + finalEvent.occupancy(),
                "The initial visitor and arriving visitor must both remain accounted for");
        assertTrue(snapshots.stream().allMatch(s -> s.occupancy() >= 0),
                "Visitor movement must never create negative occupancy");
    }


    @Test
    void shouldRejectEventWhenNoDirectedRouteFromAnEntranceExists() {
        Zone entrance = routeZone(1, "Entrance", 10, 0);
        Zone isolatedEventZone = routeZone(2, "Isolated Event Hall", 10, 0);

        com.example.mergab.Model.Facility entranceFacility =
                new com.example.mergab.Model.Facility();
        entranceFacility.setType("ENTRANCE");
        entranceFacility.setZone(entrance);
        entrance.setFacilities(new java.util.HashSet<>(
                java.util.Set.of(entranceFacility)));

        LocalDateTime start = LocalDateTime.of(2026, 10, 16, 10, 0);
        ExpoEvent event = new ExpoEvent();
        event.setId(1);
        event.setName("Unreachable Event");
        event.setZone(isolatedEventZone);
        event.setExpectedDemand(1);
        event.setStartDateTime(start.plusMinutes(1));
        event.setEndDateTime(start.plusMinutes(20));

        EventDrivenSimulationRunner runner = new EventDrivenSimulationRunner(
                new CrowdSimulationEngine(), new EventDrivenSimulationEngine());

        assertThrows(IllegalArgumentException.class, () -> runner.runSimulation(
                List.of(entrance, isolatedEventZone), List.of(), List.of(event),
                start, 5, 1),
                "The simulation should reject an event that visitors cannot reach from an entrance");
    }

    private Zone routeZone(int id, String name, int capacity, int initialVisitors) {
        Zone zone = new Zone();
        zone.setId(id);
        zone.setName(name);
        zone.setCapacity(capacity);
        zone.setInitialVisitors(initialVisitors);
        zone.setFacilities(new java.util.HashSet<>());
        return zone;
    }

    private com.example.mergab.Model.ZoneConnection routeConnection(
            int id, Zone from, Zone to, int throughput) {
        com.example.mergab.Model.ZoneConnection connection =
                new com.example.mergab.Model.ZoneConnection();
        connection.setId(id);
        connection.setFromZone(from);
        connection.setToZone(to);
        connection.setMaxVisitorsPerMinute(throughput);
        return connection;
    }

    private SimulationSnapshotRecord snapshotAt(
            List<SimulationSnapshotRecord> snapshots, int minute, int zoneId) {
        return snapshots.stream()
                .filter(s -> s.minute() == minute && s.zoneId() == zoneId)
                .findFirst()
                .orElseThrow();
    }


    @Test
    void shouldSendEventAttendeesTowardEntranceAfterEventEnds() {
        Zone entrance = routeZone(1, "Entrance", 50, 0);
        Zone corridor = routeZone(2, "Corridor", 50, 0);
        Zone eventZone = routeZone(3, "Event Hall", 50, 0);
        com.example.mergab.Model.Facility entranceFacility = new com.example.mergab.Model.Facility();
        entranceFacility.setType("ENTRANCE");
        entranceFacility.setZone(entrance);
        entrance.setFacilities(new java.util.HashSet<>(java.util.Set.of(entranceFacility)));

        List<com.example.mergab.Model.ZoneConnection> connections = List.of(
                routeConnection(11, entrance, corridor, 10),
                routeConnection(12, corridor, eventZone, 10),
                routeConnection(13, eventZone, corridor, 10),
                routeConnection(14, corridor, entrance, 10));
        LocalDateTime start = LocalDateTime.of(2026, 10, 16, 10, 0);
        ExpoEvent event = new ExpoEvent();
        event.setId(11);
        event.setName("Short event");
        event.setZone(eventZone);
        event.setExpectedDemand(3);
        event.setStartDateTime(start.plusMinutes(3));
        event.setEndDateTime(start.plusMinutes(6));

        EventDrivenSimulationRunner runner = new EventDrivenSimulationRunner(
                new CrowdSimulationEngine(), new EventDrivenSimulationEngine());
        List<SimulationSnapshotRecord> snapshots = runner.runSimulation(
                List.of(entrance, corridor, eventZone), connections, List.of(event), start, 12, 3, 42L);

        assertEquals(0, snapshotAt(snapshots, 12, 1).occupancy());
        assertEquals(0, snapshotAt(snapshots, 12, 2).occupancy());
        assertEquals(0, snapshotAt(snapshots, 12, 3).occupancy());
        assertTrue(snapshots.stream().allMatch(s -> s.occupancy() >= 0));
    }

    @Test
    void shouldProduceTheSameSnapshotsForTheSameRandomSeed() {
        Zone first = routeZone(1, "First", 2, 2);
        Zone second = routeZone(2, "Second", 2, 2);
        Zone destination = routeZone(3, "Destination", 1, 0);
        List<com.example.mergab.Model.ZoneConnection> connections = List.of(
                routeConnection(21, first, destination, 1),
                routeConnection(22, second, destination, 1));
        EventDrivenSimulationRunner runner = new EventDrivenSimulationRunner(
                new CrowdSimulationEngine(), new EventDrivenSimulationEngine());
        LocalDateTime start = LocalDateTime.of(2026, 10, 16, 10, 0);

        List<SimulationSnapshotRecord> firstRun = runner.runSimulation(
                List.of(first, second, destination), connections, List.of(), start, 5, 5, 1234L);
        List<SimulationSnapshotRecord> secondRun = runner.runSimulation(
                List.of(first, second, destination), connections, List.of(), start, 5, 5, 1234L);

        assertEquals(firstRun, secondRun);
    }
}
