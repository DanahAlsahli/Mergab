package com.example.mergab.Simulation;

import com.example.mergab.Model.Facility;
import com.example.mergab.Model.Zone;
import com.example.mergab.Model.ZoneConnection;

import java.util.*;

/**
 * Venue visitors are not event-bound. They arrive throughout the day, spend
 * different amounts of time browsing, visit multiple places and leave gradually.
 * All inter-zone trips use configured directed connections and their shared
 * minute-by-minute capacities. The model uses aggregates, not personal records.
 */
final class OrdinaryVisitorFlow {
    private static final int MOVEMENT_LIMIT_PER_ZONE = 12;
    private final Map<Integer, Deque<VisitGroup>> byZone = new HashMap<>();
    private final List<Integer> zoneIds = new ArrayList<>();
    private final List<Integer> entryZones = new ArrayList<>();
    private final Random random;
    private final int ambientBatch;
    private int nextOrigin;

    OrdinaryVisitorFlow(List<Zone> zones, SimulationState initial, long seed) {
        random = new Random(seed ^ 0x54BF18E72L);
        int totalInitial = 0;
        for (Zone zone : zones) {
            int id = zone.getId();
            zoneIds.add(id);
            int count = initial.occupancy().getOrDefault(id, 0);
            totalInitial += count;
            byZone.put(id, new ArrayDeque<>());
            if (hasFacility(zone, "ENTRANCE")) entryZones.add(id);
            addVisitors(id, count, 0, false);
        }
        Collections.sort(zoneIds);
        Collections.sort(entryZones);
        // Background expo attendance is separate from programmed events.
        // This scales to the scenario size without introducing a new API field.
        ambientBatch = totalInitial == 0 ? 0 : Math.max(1, Math.min(14, totalInitial / 24));
    }

    private static boolean hasFacility(Zone zone, String type) {
        if (zone.getFacilities() == null) return false;
        for (Facility f : zone.getFacilities()) {
            if (f != null && type.equalsIgnoreCase(f.getType())) return true;
        }
        return false;
    }

    /** Convert attendees into regular visitors after their event finishes. */
    void addVisitors(int zoneId, int count, int minute, boolean afterEvent) {
        Deque<VisitGroup> groups = byZone.computeIfAbsent(zoneId, ignored -> new ArrayDeque<>());
        while (count > 0) {
            int chunk = Math.min(count, 3 + random.nextInt(5));
            count -= chunk;
            // Some people spend 2+ hours at one zone; others browse briefly.
            int browseMinutes = 25 + random.nextInt(65);
            if (random.nextInt(4) == 0) browseMinutes += 70 + random.nextInt(95);
            if (afterEvent) browseMinutes += 15 + random.nextInt(40);
            int tripDuration = (afterEvent ? 85 : 125) + random.nextInt(260);
            groups.addLast(new VisitGroup(chunk, minute + browseMinutes,
                    minute + Math.max(browseMinutes + 15, tripDuration), -1, 0));
        }
    }

    SimulationState advanceMinute(SimulationState state, List<Zone> zones,
                                  List<ZoneConnection> connections,
                                  Map<Integer, Integer> capacities,
                                  Map<Integer, Integer> protectedVisitors,
                                  Map<ZoneConnection, Integer> corridorBudget,
                                  int minute) {
        Map<Integer, Integer> occupancy = new HashMap<>(state.occupancy());
        // A prayer coordinator may temporarily borrow regular visitors. Keep
        // our tracked groups consistent with the authoritative zone counts.
        for (int id : zoneIds) {
            int ordinary = Math.max(0, occupancy.getOrDefault(id, 0)
                    - protectedVisitors.getOrDefault(id, 0));
            Deque<VisitGroup> groups = byZone.get(id);
            int tracked = 0;
            for (VisitGroup g : groups) tracked += g.count;
            if (tracked < ordinary) addVisitors(id, ordinary - tracked, minute, false);
            if (tracked > ordinary) {
                int excess = tracked - ordinary;
                Iterator<VisitGroup> it = groups.descendingIterator();
                while (excess > 0 && it.hasNext()) {
                    VisitGroup g = it.next();
                    int removed = Math.min(excess, g.count);
                    g.count -= removed;
                    excess -= removed;
                    if (g.count == 0) it.remove();
                }
            }
        }

        // Simulate all-day walk-in visits. Where no ENTRANCE facility was
        // configured, each zone acts as an external arrival boundary rather
        // than inventing a connection between zones.
        if (ambientBatch > 0 && minute % 4 == 0 && !zoneIds.isEmpty()) {
            List<Integer> origins = entryZones.isEmpty() ? zoneIds : entryZones;
            int origin = origins.get(nextOrigin++ % origins.size());
            int admitted = Math.min(ambientBatch,
                    Math.max(0, capacities.get(origin) - occupancy.getOrDefault(origin, 0)));
            if (admitted > 0) {
                occupancy.merge(origin, admitted, Integer::sum);
                addVisitors(origin, admitted, minute, false);
            }
        }

        // People leave progressively when their overall visit is finished.
        // In scenarios with real EXIT zones, guests navigate there by directed
        // connections; without exit metadata, an aggregate boundary departure
        // is used instead of keeping guests in the venue forever.
        Set<Integer> exits = new HashSet<>();
        for (Zone zone : zones) if (hasFacility(zone, "EXIT")) exits.add(zone.getId());
        if (exits.isEmpty()) {
            for (int id : zoneIds) {
                Iterator<VisitGroup> it = byZone.get(id).iterator();
                while (it.hasNext()) {
                    VisitGroup g = it.next();
                    if (g.exitMinute <= minute) {
                        occupancy.merge(id, -g.count, Integer::sum);
                        it.remove();
                    }
                }
            }
        } else {
            for (int id : zoneIds) {
                if (!exits.contains(id) && canReachExit(id, exits, connections)) continue;
                Iterator<VisitGroup> it = byZone.get(id).iterator();
                while (it.hasNext()) {
                    VisitGroup g = it.next();
                    if (g.exitMinute <= minute) {
                        occupancy.merge(id, -g.count, Integer::sum);
                        it.remove();
                    }
                }
            }
        }

        // Work on a fixed snapshot: no visitor traverses two connections in
        // the same minute, regardless of zone ordering.
        Map<Integer, List<VisitGroup>> ready = new HashMap<>();
        for (int id : zoneIds) {
            List<VisitGroup> list = new ArrayList<>();
            for (VisitGroup g : byZone.get(id)) {
                if (minute >= g.readyMinute) list.add(g);
            }
            Collections.shuffle(list, random);
            ready.put(id, list);
        }
        for (int id : zoneIds) {
            int movedFromZone = 0;
            for (VisitGroup g : ready.get(id)) {
                if (g.count <= 0 || movedFromZone >= MOVEMENT_LIMIT_PER_ZONE) continue;
                boolean headingForExit = g.exitMinute <= minute && !exits.isEmpty();
                List<ZoneConnection> choices = new ArrayList<>();
                for (ZoneConnection edge : connections) {
                    if (edge == null || edge.getFromZone() == null || edge.getToZone() == null) continue;
                    if (!Objects.equals(edge.getFromZone().getId(), id)) continue;
                    int destination = edge.getToZone().getId();
                    if (destination == id || !capacities.containsKey(destination)) continue;
                    if (corridorBudget.getOrDefault(edge, 0) <= 0) continue;
                    if (capacities.get(destination) <= occupancy.getOrDefault(destination, 0)) continue;
                    if (headingForExit && !canReachExit(destination, exits, connections)) continue;
                    choices.add(edge);
                }
                if (choices.isEmpty()) continue;
                Collections.shuffle(choices, random);
                choices.sort(Comparator.comparingDouble(edge -> {
                    int target = edge.getToZone().getId();
                    double utilization = (double) occupancy.getOrDefault(target, 0) / capacities.get(target);
                    double revisiting = target == g.previousZone ? 0.8 : 0;
                    double exitDistance = headingForExit ? shortestExitDistance(target, exits, connections) * 3.0 : 0;
                    return utilization + revisiting + exitDistance;
                }));
                ZoneConnection selected = choices.get(0);
                int target = selected.getToZone().getId();
                int transfer = Math.min(g.count, Math.min(MOVEMENT_LIMIT_PER_ZONE - movedFromZone,
                        Math.min(corridorBudget.get(selected),
                                capacities.get(target) - occupancy.getOrDefault(target, 0))));
                if (transfer <= 0) continue;
                g.count -= transfer;
                if (g.count == 0) byZone.get(id).remove(g);
                occupancy.merge(id, -transfer, Integer::sum);
                occupancy.merge(target, transfer, Integer::sum);
                corridorBudget.put(selected, corridorBudget.get(selected) - transfer);
                int browse = headingForExit ? 1 : 25 + random.nextInt(75)
                        + (random.nextInt(5) == 0 ? 90 + random.nextInt(70) : 0);
                byZone.get(target).addLast(new VisitGroup(transfer, minute + browse,
                        g.exitMinute, id, g.visited + 1));
                movedFromZone += transfer;
            }
        }
        return new SimulationState(occupancy, state.queueLengths());
    }

    private static boolean canReachExit(int start, Set<Integer> exits,
                                        List<ZoneConnection> connections) {
        return shortestExitDistance(start, exits, connections) < 100000;
    }

    private static int shortestExitDistance(int start, Set<Integer> exits,
                                             List<ZoneConnection> connections) {
        if (exits.contains(start)) return 0;
        Set<Integer> seen = new HashSet<>();
        Deque<Integer> queue = new ArrayDeque<>();
        queue.add(start); seen.add(start);
        int steps = 0;
        while (!queue.isEmpty()) {
            int count = queue.size();
            for (int i = 0; i < count; i++) {
                int at = queue.removeFirst();
                for (ZoneConnection edge : connections) {
                    if (edge == null || edge.getFromZone() == null || edge.getToZone() == null
                            || !Objects.equals(edge.getFromZone().getId(), at)) continue;
                    int to = edge.getToZone().getId();
                    if (exits.contains(to)) return steps + 1;
                    if (seen.add(to)) queue.addLast(to);
                }
            }
            steps++;
        }
        return 100000;
    }

    private static final class VisitGroup {
        int count;
        final int readyMinute;
        final int exitMinute;
        final int previousZone;
        final int visited;
        VisitGroup(int count, int readyMinute, int exitMinute, int previousZone, int visited) {
            this.count = count;
            this.readyMinute = readyMinute;
            this.exitMinute = exitMinute;
            this.previousZone = previousZone;
            this.visited = visited;
        }
    }
}
