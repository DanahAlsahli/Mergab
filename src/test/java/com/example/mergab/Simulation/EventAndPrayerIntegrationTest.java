package com.example.mergab.Simulation;

import com.example.mergab.Model.ExpoEvent;
import com.example.mergab.Model.Facility;
import com.example.mergab.Model.Zone;
import com.example.mergab.Model.ZoneConnection;
import com.example.mergab.Service.PrayerTimesService;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class EventAndPrayerIntegrationTest {

    @Test
    void eventAndPrayerMovementCanRunTogetherWithoutExceedingZoneCapacity() {
        Zone entrance = zone(1, "Entrance Area", 50, 20, false);
        Zone corridor = zone(2, "Main Corridor", 50, 0, false);
        Zone eventZone = zone(3, "Event Hall", 50, 0, false);
        Zone prayerZone = zone(4, "Prayer Area", 50, 0, true);

        Facility entranceFacility = new Facility();
        entranceFacility.setId(1);
        entranceFacility.setName("Main Entrance");
        entranceFacility.setType("ENTRANCE");
        entranceFacility.setZone(entrance);
        entrance.setFacilities(new HashSet<>(Set.of(entranceFacility)));

        Facility prayerFacility = new Facility();
        prayerFacility.setId(2);
        prayerFacility.setName("Prayer Room");
        prayerFacility.setType("PRAYER_ROOM");
        prayerFacility.setZone(prayerZone);
        prayerZone.setFacilities(new HashSet<>(Set.of(prayerFacility)));

        List<Zone> zones = List.of(entrance, corridor, eventZone, prayerZone);
        List<ZoneConnection> connections = List.of(
                connection(1, entrance, corridor, 5),
                connection(2, corridor, entrance, 5),
                connection(3, corridor, eventZone, 5),
                connection(4, eventZone, corridor, 5),
                connection(5, corridor, prayerZone, 5),
                connection(6, prayerZone, corridor, 5));

        LocalDateTime start = LocalDateTime.of(2026, 10, 15, 11, 50);
        ExpoEvent event = new ExpoEvent();
        event.setId(1);
        event.setName("Scheduled Event");
        event.setZone(eventZone);
        event.setExpectedDemand(6);
        event.setStartDateTime(start.plusMinutes(5));
        event.setEndDateTime(start.plusMinutes(30));

        PrayerTimesService prayerTimes = mock(PrayerTimesService.class);
        when(prayerTimes.getTodayTimes()).thenReturn(Map.of(
                "fajr", "04:30", "dhuhr", "12:00", "asr", "15:00",
                "maghrib", "18:00", "isha", "19:30"));

        EventDrivenSimulationRunner runner = new EventDrivenSimulationRunner(
                new CrowdSimulationEngine(), new EventDrivenSimulationEngine(), prayerTimes);

        List<SimulationSnapshotRecord> snapshots = runner.runSimulation(
                zones, connections, List.of(event), start, 15, 5);

        assertTrue(snapshots.stream().anyMatch(s -> s.zoneId() == prayerZone.getId()
                && s.occupancy() > 0), "Some selected visitors should reach the prayer zone");
        assertTrue(snapshots.stream().anyMatch(s -> s.zoneId() == eventZone.getId()
                && s.occupancy() > 0), "Event visitors should still reach the event zone");
        assertTrue(snapshots.stream().allMatch(s -> s.occupancy() >= 0
                && s.occupancy() <= zones.stream()
                        .filter(z -> z.getId().equals(s.zoneId()))
                        .findFirst().orElseThrow().getCapacity()),
                "Zone occupancy must stay within configured capacity");
    }

    private static Zone zone(int id, String name, int capacity, int initial, boolean prayer) {
        Zone zone = new Zone();
        zone.setId(id);
        zone.setName(name);
        zone.setCapacity(capacity);
        zone.setInitialVisitors(initial);
        zone.setHasPrayerArea(prayer);
        zone.setFacilities(new HashSet<>());
        return zone;
    }

    private static ZoneConnection connection(int id, Zone from, Zone to, int throughput) {
        ZoneConnection connection = new ZoneConnection();
        connection.setId(id);
        connection.setFromZone(from);
        connection.setToZone(to);
        connection.setMaxVisitorsPerMinute(throughput);
        return connection;
    }
}
