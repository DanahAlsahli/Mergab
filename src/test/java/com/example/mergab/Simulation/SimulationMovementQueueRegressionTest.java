package com.example.mergab.Simulation;

import com.example.mergab.Model.ExpoEvent;
import com.example.mergab.Model.Facility;
import com.example.mergab.Model.Zone;
import com.example.mergab.Model.ZoneConnection;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class SimulationMovementQueueRegressionTest {
    @Test
    void narrowEventCorridorCreatesSourceQueueWithoutBreakingCapacity() {
        Zone entrance = zone(1, "Entrance", 100, 0);
        Zone hall = zone(2, "Hall", 100, 0);
        Zone exit = zone(3, "Exit", 100, 0);
        Facility entry = new Facility();
        entry.setType("ENTRANCE");
        entry.setZone(entrance);
        entrance.setFacilities(Set.of(entry));
        Facility departure = new Facility();
        departure.setType("EXIT");
        departure.setZone(exit);
        exit.setFacilities(Set.of(departure));
        ZoneConnection incoming = connection(entrance, hall, 1);
        ZoneConnection outgoing = connection(hall, exit, 1);
        LocalDateTime start = LocalDateTime.of(2026, 10, 20, 12, 0);
        ExpoEvent event = new ExpoEvent();
        event.setZone(hall);
        event.setExpectedDemand(20);
        event.setStartDateTime(start.plusMinutes(10));
        event.setEndDateTime(start.plusMinutes(15));

        List<SimulationSnapshotRecord> records = new EventDrivenSimulationRunner(
                new CrowdSimulationEngine(), new EventDrivenSimulationEngine())
                .runSimulation(List.of(entrance, hall, exit),
                        List.of(incoming, outgoing), List.of(event), start, 30, 5, 42L);

        assertTrue(records.stream().anyMatch(s -> s.zoneId() == 1
                && s.minute() >= 6 && s.minute() <= 15 && s.queueLength() > 0),
                "Event arrivals must queue physically at the narrow corridor source");
        assertTrue(records.stream().allMatch(s -> s.occupancy() >= 0
                && s.occupancy() <= 100 && s.queueLength() >= 0));
        assertTrue(records.stream().anyMatch(s -> s.zoneId() == 2
                && s.minute() >= 15 && s.queueLength() > 0),
                "Departing attendees must wait at the hall exit corridor");
    }

    @Test
    void ordinaryVisitorsDoNotReturnFromExitToVenue() {
        Zone entrance = zone(1, "Entrance", 100, 0);
        Zone plaza = zone(2, "Exit", 100, 8);
        Facility entry = new Facility();
        entry.setType("ENTRANCE");
        entry.setZone(entrance);
        entrance.setFacilities(Set.of(entry));
        Facility exitFacility = new Facility();
        exitFacility.setType("EXIT");
        exitFacility.setZone(plaza);
        plaza.setFacilities(Set.of(exitFacility));
        List<SimulationSnapshotRecord> records = new EventDrivenSimulationRunner(
                new CrowdSimulationEngine(), new EventDrivenSimulationEngine())
                .runSimulation(List.of(entrance, plaza),
                        List.of(connection(plaza, entrance, 8), connection(entrance, plaza, 8)),
                        List.of(), LocalDateTime.of(2026, 10, 20, 12, 0), 3, 5);
        assertEquals(0, records.stream().filter(s -> s.minute() == 3 && s.zoneId() == 1)
                .findFirst().orElseThrow().occupancy());
    }

    @Test
    void ordinaryVisitorsHaveDwellTimeBeforeBeginningGradualExit() {
        Zone entrance = zone(1, "Entrance", 100, 20);
        Zone exit = zone(2, "Exit", 100, 0);
        Facility entry = new Facility();
        entry.setType("ENTRANCE");
        entry.setZone(entrance);
        entrance.setFacilities(Set.of(entry));
        Facility exitFacility = new Facility();
        exitFacility.setType("EXIT");
        exitFacility.setZone(exit);
        exit.setFacilities(Set.of(exitFacility));
        List<SimulationSnapshotRecord> records = new EventDrivenSimulationRunner(
                new CrowdSimulationEngine(), new EventDrivenSimulationEngine())
                .runSimulation(List.of(entrance, exit),
                        List.of(connection(entrance, exit, 10), connection(exit, entrance, 10)),
                        List.of(), LocalDateTime.of(2026, 10, 20, 12, 0), 45, 5, 42L);
        assertEquals(20, records.stream().filter(s -> s.minute() == 15 && s.zoneId() == 1)
                .findFirst().orElseThrow().occupancy(),
                "Background visitors should be able to remain in the venue initially");
        assertTrue(records.stream().anyMatch(s -> s.minute() >= 25 && s.zoneId() == 1
                && s.occupancy() < 20), "Background visitors should eventually depart");
        assertTrue(records.stream().allMatch(s -> s.occupancy() >= 0 && s.queueLength() >= 0));
    }

    private Zone zone(int id, String name, int capacity, int initial) {
        Zone zone = new Zone();
        zone.setId(id);
        zone.setName(name);
        zone.setCapacity(capacity);
        zone.setInitialVisitors(initial);
        zone.setFacilities(Set.of());
        return zone;
    }

    private ZoneConnection connection(Zone from, Zone to, int perMinute) {
        ZoneConnection connection = new ZoneConnection();
        connection.setFromZone(from);
        connection.setToZone(to);
        connection.setMaxVisitorsPerMinute(perMinute);
        return connection;
    }
}
