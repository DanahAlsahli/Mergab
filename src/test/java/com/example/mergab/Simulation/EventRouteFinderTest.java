
package com.example.mergab.Simulation;

import com.example.mergab.Model.Facility;
import com.example.mergab.Model.Zone;
import com.example.mergab.Model.ZoneConnection;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class EventRouteFinderTest {

    @Test
    void shouldFindDirectedRouteFromEntranceToEvent() {
        Zone entrance = createZone(1, "Entrance");
        Zone corridor = createZone(2, "Corridor");
        Zone eventZone = createZone(3, "Event");

        Facility facility = new Facility();
        facility.setType("ENTRANCE");
        facility.setZone(entrance);
        entrance.setFacilities(java.util.Set.of(facility));

        ZoneConnection first = connection(1, entrance, corridor);
        ZoneConnection second = connection(2, corridor, eventZone);

        EventRouteFinder finder = new EventRouteFinder();

        List<ZoneConnection> route = finder.findRoute(
                List.of(entrance, corridor, eventZone),
                List.of(first, second),
                eventZone
        ).orElseThrow();

        assertEquals(2, route.size());
        assertEquals(first, route.get(0));
        assertEquals(second, route.get(1));
    }

    @Test
    void shouldReturnEmptyWhenNoDirectedRouteExists() {
        Zone entrance = createZone(1, "Entrance");
        Zone eventZone = createZone(2, "Event");

        Facility facility = new Facility();
        facility.setType("ENTRANCE");
        facility.setZone(entrance);
        entrance.setFacilities(java.util.Set.of(facility));

        // The connection points away from the entrance.
        ZoneConnection connection = connection(
                1, eventZone, entrance
        );

        EventRouteFinder finder = new EventRouteFinder();

        assertTrue(finder.findRoute(
                List.of(entrance, eventZone),
                List.of(connection),
                eventZone
        ).isEmpty());
    }

    private Zone createZone(int id, String name) {
        Zone zone = new Zone();
        zone.setId(id);
        zone.setName(name);
        zone.setCapacity(100);
        zone.setInitialVisitors(0);
        zone.setFacilities(new java.util.HashSet<>());
        return zone;
    }

    private ZoneConnection connection(
            int id, Zone from, Zone to) {

        ZoneConnection connection = new ZoneConnection();
        connection.setId(id);
        connection.setFromZone(from);
        connection.setToZone(to);
        connection.setMaxVisitorsPerMinute(10);
        return connection;
    }
}
