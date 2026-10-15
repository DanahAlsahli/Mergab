
package com.example.mergab.Simulation;

import com.example.mergab.Model.ExpoEvent;
import com.example.mergab.Model.Zone;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

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
}
