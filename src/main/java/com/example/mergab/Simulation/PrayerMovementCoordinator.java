package com.example.mergab.Simulation;

import com.example.mergab.Model.Facility;
import com.example.mergab.Model.Zone;
import com.example.mergab.Model.ZoneConnection;

import java.util.*;

/** Coordinates aggregate visitor cohorts travelling to prayer-room facilities. */
public class PrayerMovementCoordinator {
    public static final int PRE_PRAYER_MINUTES = 10;
    public static final int RETURN_AFTER_PRAYER_MINUTES = 15;
    public static final double PARTICIPATION_RATE = 0.30;

    private final List<Cohort> cohorts = new ArrayList<>();
    private final Set<String> triggeredPrayerDates = new HashSet<>();

    /** Visitors in prayer cohorts are protected from ordinary random movement. */
    public Map<Integer, Integer> getProtectedVisitors() {
        Map<Integer, Integer> result = new HashMap<>();
        for (Cohort cohort : cohorts) {
            if (cohort.count <= 0) continue;
            List<Zone> path = cohort.returning ? cohort.returnPath : cohort.outbound;

            // Once a returning cohort reaches its original zone, it is no longer
            // on a reserved prayer journey and may participate in ordinary movement.
            if (cohort.returning && cohort.step >= path.size() - 1) continue;
            if (cohort.step < 0 || cohort.step >= path.size()) continue;

            int zoneId = path.get(cohort.step).getId();
            result.merge(zoneId, cohort.count, Integer::sum);
        }
        return result;
    }

    /** Trigger and advance prayer cohorts for one simulated minute. */
    public SimulationState advanceMinute(
            SimulationState state,
            List<Zone> zones,
            List<ZoneConnection> connections,
            Map<Integer, Integer> capacities,
            Map<String, String> prayerTimes,
            int minuteOfDay,
            String simulationDate,
            Map<Integer, Integer> alreadyReservedVisitors) {

        Objects.requireNonNull(state, "Simulation state is required");
        if (prayerTimes == null || prayerTimes.isEmpty()) return state;

        Map<Integer, Zone> zonesById = new HashMap<>();
        for (Zone zone : zones) zonesById.put(zone.getId(), zone);

        SimulationState current = moveCohorts(state, connections, capacities, minuteOfDay);
        String[] prayerKeys = {"fajr", "dhuhr", "asr", "maghrib", "isha"};
        for (String key : prayerKeys) {
            String time = prayerTimes.get(key);
            if (time == null) continue;
            int prayerMinute;
            try { prayerMinute = PrayerMinuteParser.toMinute(time); }
            catch (RuntimeException ignored) { continue; }
            int triggerMinute = Math.floorMod(prayerMinute - PRE_PRAYER_MINUTES, 1440);
            String triggerKey = simulationDate + ":" + key;
            if (minuteOfDay == triggerMinute && triggeredPrayerDates.add(triggerKey)) {
                current = startPrayerCohorts(current, zones, connections, capacities,
                        prayerMinute, simulationDate, key, alreadyReservedVisitors);
            }
        }
        return current;
    }

    private SimulationState startPrayerCohorts(
            SimulationState state, List<Zone> zones, List<ZoneConnection> connections,
            Map<Integer, Integer> capacities, int prayerMinute, String date, String prayerKey,
            Map<Integer, Integer> alreadyReservedVisitors) {
        List<Zone> prayerZones = zones.stream()
                .filter(z -> Boolean.TRUE.equals(z.getHasPrayerArea()))
                .filter(z -> hasPrayerRoom(z))
                .toList();
        if (prayerZones.isEmpty()) return state;

        Map<Integer, Integer> occupancy = new HashMap<>(state.occupancy());
        List<Zone> sources = zones.stream()
                .filter(z -> !Boolean.TRUE.equals(z.getHasPrayerArea()))
                .sorted(Comparator.comparing(Zone::getId))
                .toList();

        for (Zone source : sources) {
            int sourceId = source.getId();
            int present = occupancy.getOrDefault(sourceId, 0);
            int eligible = Math.max(0, present - alreadyReservedVisitors.getOrDefault(sourceId, 0));
            if (eligible <= 0) continue;
            int selected = (int) Math.floor(eligible * PARTICIPATION_RATE);
            if (selected <= 0) continue;

            RouteChoice best = null;
            for (Zone target : prayerZones) {
                List<Zone> path = shortestPath(source, target, zones, connections);
                if (path.isEmpty()) continue;
                if (best == null || path.size() < best.outbound.size()) {
                    List<Zone> returnPath = shortestPath(target, source, zones, connections);
                    if (!returnPath.isEmpty()) best = new RouteChoice(target, path, returnPath);
                }
            }
            if (best == null) continue;

            // Keep selected visitors in occupancy; the cohort reservation prevents ordinary movement.
            cohorts.add(new Cohort(selected, best.outbound, best.returnPath,
                    prayerMinute, date, prayerKey));
        }
        return new SimulationState(occupancy, state.queueLengths());
    }

    private SimulationState moveCohorts(SimulationState state, List<ZoneConnection> connections,
                                        Map<Integer, Integer> capacities, int minuteOfDay) {
        Map<Integer, Integer> occupancy = new HashMap<>(state.occupancy());
        Map<ZoneConnection, Integer> corridorRemaining = new HashMap<>();
        for (ZoneConnection c : connections) {
            corridorRemaining.put(c, Math.max(0, c.getMaxVisitorsPerMinute() == null ? 0 : c.getMaxVisitorsPerMinute()));
        }

        for (Cohort cohort : new ArrayList<>(cohorts)) {
            if (cohort.count <= 0) continue;

            // A completed return journey releases its visitors from the protected cohort.
            if (cohort.returning && cohort.step >= cohort.returnPath.size() - 1) {
                cohort.count = 0;
                continue;
            }

            if (!cohort.returning && cohort.step >= cohort.outbound.size() - 1) {
                if (cohort.arrivedAtMinute < 0) cohort.arrivedAtMinute = minuteOfDay;
                // Wait until 15 minutes after the scheduled prayer time.
                int leaveAt = Math.floorMod(cohort.prayerMinute + RETURN_AFTER_PRAYER_MINUTES, 1440);
                int minutesSincePrayer = Math.floorMod(minuteOfDay - cohort.prayerMinute, 1440);
                if (minutesSincePrayer >= RETURN_AFTER_PRAYER_MINUTES && minutesSincePrayer < 720) {
                    cohort.returning = true;
                    cohort.step = 0;
                }
            }
            List<Zone> path = cohort.returning ? cohort.returnPath : cohort.outbound;
            if (path.size() < 2 || cohort.step >= path.size() - 1) continue;
            Zone from = path.get(cohort.step);
            Zone to = path.get(cohort.step + 1);
            ZoneConnection edge = findConnection(connections, from.getId(), to.getId());
            if (edge == null) continue;
            int free = Math.max(0, capacities.getOrDefault(to.getId(), 0) - occupancy.getOrDefault(to.getId(), 0));
            int throughput = corridorRemaining.getOrDefault(edge, 0);
            int moved = Math.min(cohort.count, Math.min(free, throughput));
            if (moved <= 0) continue;
            occupancy.put(from.getId(), occupancy.getOrDefault(from.getId(), 0) - moved);
            occupancy.put(to.getId(), occupancy.getOrDefault(to.getId(), 0) + moved);
            corridorRemaining.put(edge, throughput - moved);
            // A cohort is aggregate; partial movement is represented as a separate cohort.
            cohort.count -= moved;
            cohorts.add(new Cohort(moved, path, cohort.returnPath, cohort.prayerMinute,
                    cohort.date, cohort.prayerKey, cohort.returning, cohort.step + 1, cohort.arrivedAtMinute));
        }
        cohorts.removeIf(c -> c.count <= 0);
        return new SimulationState(occupancy, state.queueLengths());
    }

    private boolean hasPassed(int arrivedAt, int now, int leaveAt) {
        // Handles a prayer period that crosses midnight without comparing wall-clock dates.
        int elapsed = Math.floorMod(now - arrivedAt, 1440);
        int wait = Math.floorMod(leaveAt - arrivedAt, 1440);
        return elapsed >= wait;
    }

    private static boolean hasPrayerRoom(Zone zone) {
        if (zone.getFacilities() == null) return false;
        return zone.getFacilities().stream().anyMatch(f -> f != null
                && "PRAYER_ROOM".equalsIgnoreCase(f.getType()));
    }

    private static List<Zone> shortestPath(Zone source, Zone target, List<Zone> zones,
                                           List<ZoneConnection> connections) {
        if (source.getId().equals(target.getId())) return List.of(source);
        Map<Integer, List<Integer>> outgoing = new HashMap<>();
        for (ZoneConnection c : connections) {
            if (c != null && c.getFromZone() != null && c.getToZone() != null
                    && c.getFromZone().getId() != null && c.getToZone().getId() != null) {
                outgoing.computeIfAbsent(c.getFromZone().getId(), k -> new ArrayList<>())
                        .add(c.getToZone().getId());
            }
        }
        Map<Integer, Integer> previous = new HashMap<>();
        Queue<Integer> queue = new ArrayDeque<>();
        Set<Integer> visited = new HashSet<>();
        queue.add(source.getId()); visited.add(source.getId());
        while (!queue.isEmpty()) {
            int current = queue.remove();
            if (current == target.getId()) break;
            for (int next : outgoing.getOrDefault(current, List.of())) {
                if (visited.add(next)) { previous.put(next, current); queue.add(next); }
            }
        }
        if (!visited.contains(target.getId())) return List.of();
        LinkedList<Integer> ids = new LinkedList<>();
        int current = target.getId(); ids.addFirst(current);
        while (current != source.getId()) { current = previous.get(current); ids.addFirst(current); }
        Map<Integer, Zone> byId = new HashMap<>(); for (Zone z : zones) byId.put(z.getId(), z);
        return ids.stream().map(byId::get).filter(Objects::nonNull).toList();
    }

    private static ZoneConnection findConnection(List<ZoneConnection> connections, int from, int to) {
        return connections.stream().filter(c -> c != null && c.getFromZone() != null && c.getToZone() != null
                && Objects.equals(c.getFromZone().getId(), from) && Objects.equals(c.getToZone().getId(), to))
                .findFirst().orElse(null);
    }

    private record RouteChoice(Zone target, List<Zone> outbound, List<Zone> returnPath) {}

    private static final class Cohort {
        private int count; private final List<Zone> outbound; private final List<Zone> returnPath;
        private final int prayerMinute; private final String date; private final String prayerKey;
        private boolean returning; private int step; private int arrivedAtMinute;
        private Cohort(int count, List<Zone> outbound, List<Zone> returnPath, int prayerMinute, String date, String prayerKey) {
            this(count, outbound, returnPath, prayerMinute, date, prayerKey, false, 0, -1);
        }
        private Cohort(int count, List<Zone> outbound, List<Zone> returnPath, int prayerMinute, String date, String prayerKey,
                       boolean returning, int step, int arrivedAtMinute) {
            this.count=count; this.outbound=outbound; this.returnPath=returnPath; this.prayerMinute=prayerMinute;
            this.date=date; this.prayerKey=prayerKey; this.returning=returning; this.step=step; this.arrivedAtMinute=arrivedAtMinute;
        }
    }

    private static final class PrayerMinuteParser {
        private static int toMinute(String time) {
            String[] parts = time.split(":");
            return Integer.parseInt(parts[0]) * 60 + Integer.parseInt(parts[1]);
        }
    }
}
