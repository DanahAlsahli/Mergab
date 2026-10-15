
package com.example.mergab.Simulation;

import com.example.mergab.Model.Facility;
import com.example.mergab.Model.Zone;
import com.example.mergab.Model.ZoneConnection;

import java.util.*;

public class EventRouteFinder {

    public Optional<List<ZoneConnection>> findRoute(
            List<Zone> zones,
            List<ZoneConnection> connections,
            Zone targetZone) {

        if (zones == null || connections == null || targetZone == null) {
            throw new IllegalArgumentException(
                    "Zones, connections, and target zone are required"
            );
        }

        if (targetZone.getId() == null) {
            throw new IllegalArgumentException(
                    "Target zone must have an ID"
            );
        }

        Set<Integer> entranceZoneIds = new HashSet<>();

        for (Zone zone : zones) {
            if (zone.getFacilities() == null) {
                continue;
            }

            for (Facility facility : zone.getFacilities()) {
                if (facility != null
                        && "ENTRANCE".equalsIgnoreCase(facility.getType())) {
                    entranceZoneIds.add(zone.getId());
                    break;
                }
            }
        }

        if (entranceZoneIds.isEmpty()) {
            return Optional.empty();
        }

        Map<Integer, List<ZoneConnection>> outgoing = new HashMap<>();

        for (ZoneConnection connection : connections) {
            if (connection == null
                    || connection.getFromZone() == null
                    || connection.getToZone() == null
                    || connection.getFromZone().getId() == null
                    || connection.getToZone().getId() == null) {
                continue;
            }

            outgoing.computeIfAbsent(
                    connection.getFromZone().getId(),
                    ignored -> new ArrayList<>()
            ).add(connection);
        }

        Queue<Integer> queue = new ArrayDeque<>();
        Set<Integer> visited = new HashSet<>();
        Map<Integer, ZoneConnection> previousConnection = new HashMap<>();

        for (Integer entranceId : entranceZoneIds) {
            if (visited.add(entranceId)) {
                queue.add(entranceId);
            }
        }

        Integer targetId = targetZone.getId();

        while (!queue.isEmpty()) {
            Integer currentId = queue.poll();

            if (currentId.equals(targetId)) {
                return Optional.of(
                        reconstructRoute(
                                targetId,
                                entranceZoneIds,
                                previousConnection
                        )
                );
            }

            for (ZoneConnection connection :
                    outgoing.getOrDefault(currentId, List.of())) {

                Integer nextId = connection.getToZone().getId();

                if (visited.add(nextId)) {
                    previousConnection.put(nextId, connection);
                    queue.add(nextId);
                }
            }
        }

        return Optional.empty();
    }

    private List<ZoneConnection> reconstructRoute(
            Integer targetId,
            Set<Integer> entranceZoneIds,
            Map<Integer, ZoneConnection> previousConnection) {

        LinkedList<ZoneConnection> route = new LinkedList<>();
        Integer currentId = targetId;

        while (!entranceZoneIds.contains(currentId)) {
            ZoneConnection connection =
                    previousConnection.get(currentId);

            if (connection == null) {
                throw new IllegalStateException(
                        "Could not reconstruct the discovered route"
                );
            }

            route.addFirst(connection);
            currentId = connection.getFromZone().getId();
        }

        return route;
    }
}
