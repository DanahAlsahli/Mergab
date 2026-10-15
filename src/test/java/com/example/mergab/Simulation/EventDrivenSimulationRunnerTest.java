
package com.example.mergab.Simulation;

import com.example.mergab.Model.ExpoEvent;
import com.example.mergab.Model.Zone;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

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




}
