package com.example.mergab.Simulation;

import com.example.mergab.Model.Facility;
import com.example.mergab.Model.Zone;
import com.example.mergab.Model.ZoneConnection;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PrayerMovementCoordinatorTest {

    @Test
    void selectsOnlyThirtyPercentAndDoesNotTreatPrayerFacilityAsZone() {
        Fixture f = fixture();
        PrayerMovementCoordinator coordinator = new PrayerMovementCoordinator();
        SimulationState initial = new SimulationState(
                Map.of(1, 10, 2, 0, 3, 0), Map.of(1, 0, 2, 0, 3, 0));

        SimulationState afterTrigger = coordinator.advanceMinute(
                initial, f.zones, f.connections, Map.of(1, 20, 2, 20, 3, 20),
                Map.of("dhuhr", "12:00"), 11 * 60 + 50, "2026-10-15", Map.of());

        assertEquals(10, afterTrigger.occupancy().get(1));
        assertEquals(3, coordinator.getProtectedVisitors().get(1));
        assertEquals(0, afterTrigger.occupancy().get(3));
    }

    @Test
    void prayerVisitorsMoveAlongConnectionsWithoutExceedingZoneCapacity() {
        Fixture f = fixture();
        PrayerMovementCoordinator coordinator = new PrayerMovementCoordinator();
        SimulationState state = new SimulationState(
                Map.of(1, 10, 2, 0, 3, 0), Map.of(1, 0, 2, 0, 3, 0));
        Map<Integer, Integer> capacities = Map.of(1, 20, 2, 20, 3, 20);
        Map<String, String> times = Map.of("dhuhr", "12:00");

        state = coordinator.advanceMinute(state, f.zones, f.connections, capacities,
                times, 710, "2026-10-15", Map.of());
        state = coordinator.advanceMinute(state, f.zones, f.connections, capacities,
                times, 711, "2026-10-15", Map.of());

        assertEquals(3, state.occupancy().get(2));
        assertEquals(7, state.occupancy().get(1));
        assertTrue(state.occupancy().values().stream().allMatch(v -> v >= 0 && v <= 20));
    }

    private Fixture fixture() {
        Zone entranceZone = zone(1, "Main Area", 20, false);
        Zone corridor = zone(2, "Corridor", 20, false);
        Zone prayerZone = zone(3, "Prayer Area", 20, true);
        Facility prayerRoom = new Facility();
        prayerRoom.setId(30);
        prayerRoom.setName("Prayer Room");
        prayerRoom.setType("PRAYER_ROOM");
        prayerRoom.setZone(prayerZone);
        prayerZone.setFacilities(new HashSet<>(Set.of(prayerRoom)));

        List<Zone> zones = List.of(entranceZone, corridor, prayerZone);
        List<ZoneConnection> connections = List.of(
                connection(1, entranceZone, corridor, 3),
                connection(2, corridor, prayerZone, 3),
                connection(3, prayerZone, corridor, 3),
                connection(4, corridor, entranceZone, 3));
        return new Fixture(zones, connections);
    }

    private Zone zone(int id, String name, int capacity, boolean prayer) {
        Zone zone = new Zone();
        zone.setId(id);
        zone.setName(name);
        zone.setCapacity(capacity);
        zone.setInitialVisitors(id == 1 ? 10 : 0);
        zone.setHasPrayerArea(prayer);
        zone.setFacilities(new HashSet<>());
        return zone;
    }

    private ZoneConnection connection(int id, Zone from, Zone to, int throughput) {
        ZoneConnection connection = new ZoneConnection();
        connection.setId(id);
        connection.setFromZone(from);
        connection.setToZone(to);
        connection.setMaxVisitorsPerMinute(throughput);
        return connection;
    }

    private record Fixture(List<Zone> zones, List<ZoneConnection> connections) {}
}
