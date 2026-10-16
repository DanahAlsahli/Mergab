package com.example.mergab.Service;

import com.example.mergab.DTO.RouteResponseDTO;
import com.example.mergab.Model.SimulationSnapshot;
import com.example.mergab.Model.Zone;
import com.example.mergab.Model.ZoneConnection;
import com.example.mergab.Repository.SimulationRepository;
import com.example.mergab.Repository.SimulationSnapshotRepository;
import com.example.mergab.Repository.ZoneConnectionRepository;
import com.example.mergab.Repository.ZoneRepository;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;

@Service
public class RouteRecommendationService {

    private final ZoneRepository zoneRepository;
    private final ZoneConnectionRepository zoneConnectionRepository;
    private final SimulationSnapshotRepository snapshotRepository;
    private final SimulationRepository simulationRepository;

    public RouteRecommendationService(
            ZoneRepository zoneRepository,
            ZoneConnectionRepository zoneConnectionRepository,
            SimulationSnapshotRepository snapshotRepository,
            SimulationRepository simulationRepository) {

        this.zoneRepository = zoneRepository;
        this.zoneConnectionRepository = zoneConnectionRepository;
        this.snapshotRepository = snapshotRepository;
        this.simulationRepository = simulationRepository;
    }

    public RouteResponseDTO recommendRoute(
            Integer currentZoneId,
            Integer destinationZoneId,
            Integer simulationId) {

        // Validate required inputs
        if (currentZoneId == null
                || destinationZoneId == null
                || simulationId == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Zone IDs and simulation ID are required"
            );
        }

        // Start and destination must be different
        if (currentZoneId.equals(destinationZoneId)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Start zone and destination zone must be different"
            );
        }

        // Validate that the simulation exists
        if (!simulationRepository.existsById(simulationId)) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Simulation not found"
            );
        }

        // Find start and destination zones
        Zone start = zoneRepository.findZoneById(currentZoneId);
        Zone destination = zoneRepository.findZoneById(destinationZoneId);

        if (start == null || destination == null) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Start zone or destination zone not found"
            );
        }

        List<ZoneConnection> connections =
                zoneConnectionRepository.findAll();

        List<SimulationSnapshot> snapshots =
                snapshotRepository.findBySimulation_Id(simulationId);

        Map<Integer, SimulationSnapshot> latestSnapshots =
                getLatestSnapshots(snapshots);

        Map<Integer, List<ZoneConnection>> graph = new HashMap<>();

        for (ZoneConnection connection : connections) {
            if (connection.getFromZone() == null
                    || connection.getToZone() == null) {
                continue;
            }

            graph.computeIfAbsent(
                    connection.getFromZone().getId(),
                    key -> new ArrayList<>()
            ).add(connection);
        }

        List<PathCandidate> foundPaths = findPaths(
                currentZoneId,
                destinationZoneId,
                graph,
                connections,
                latestSnapshots
        );

        if (foundPaths.isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "No route exists between the selected zones"
            );
        }

        PathCandidate best = foundPaths.get(0);

        List<RouteResponseDTO.ZoneStep> recommendedPath =
                buildPathDetails(best.zoneIds, latestSnapshots);

        List<RouteResponseDTO.ZoneStep> alternativePath =
                foundPaths.size() > 1
                        ? buildPathDetails(
                        foundPaths.get(1).zoneIds,
                        latestSnapshots)
                        : new ArrayList<>();

        String congestionLevel =
                getPathCongestionLevel(recommendedPath);

        Integer snapshotMinute = snapshots.stream()
                .map(SimulationSnapshot::getMinute)
                .filter(Objects::nonNull)
                .max(Integer::compareTo)
                .orElse(null);

        String message = alternativePath.isEmpty()
                ? "Recommended route found. No alternative route was found."
                : "Recommended route and alternative route found.";

        String destinationWarning = null;

        double destinationOccupancyRate =
                (double) getOccupancy(destination, latestSnapshots)
                        / Math.max(destination.getCapacity(), 1);

        if (destinationOccupancyRate >= 0.90) {
            destinationWarning =
                    "تحذير: الوجهة مزدحمة جدًا. يُنصح بالتأكد من إمكانية الوصول إليها.";
        } else if (destinationOccupancyRate >= 0.70) {
            destinationWarning =
                    "تنبيه: الوجهة مزدحمة نسبيًا.";
        }

        ZoneStepResult alternativeResult = null;

        if (destinationWarning != null) {
            alternativeResult = findAlternativeDestination(
                    currentZoneId,
                    destinationZoneId,
                    destinationOccupancyRate,
                    graph,
                    connections,
                    latestSnapshots
            );
        }

        RouteResponseDTO.ZoneStep alternativeDestination =
                alternativeResult == null
                        ? null
                        : alternativeResult.destination;

        List<RouteResponseDTO.ZoneStep> alternativeDestinationPath =
                alternativeResult == null
                        ? new ArrayList<>()
                        : alternativeResult.path;

        if (alternativeDestination != null) {
            message +=
                    " An alternative low-congestion destination is available.";
        } else if (destinationWarning != null) {
            message +=
                    " No reachable low-congestion alternative destination was found.";
        }

        return new RouteResponseDTO(
                "SUCCESS",
                message,
                Math.round(best.cost * 100.0) / 100.0,
                congestionLevel,
                snapshotMinute,
                recommendedPath,
                alternativePath,
                destinationWarning,
                alternativeDestination,
                alternativeDestinationPath
        );
    }

    private List<PathCandidate> findPaths(
            Integer startZoneId,
            Integer destinationZoneId,
            Map<Integer, List<ZoneConnection>> graph,
            List<ZoneConnection> connections,
            Map<Integer, SimulationSnapshot> latestSnapshots) {

        PriorityQueue<PathCandidate> queue = new PriorityQueue<>(
                Comparator.comparingDouble(path -> path.cost)
        );

        queue.offer(new PathCandidate(
                new ArrayList<>(Collections.singletonList(startZoneId)),
                0.0
        ));

        List<PathCandidate> foundPaths = new ArrayList<>();

        int explored = 0;
        final int MAX_EXPLORED = 10000;

        while (!queue.isEmpty()
                && foundPaths.size() < 2
                && explored < MAX_EXPLORED) {

            PathCandidate current = queue.poll();
            explored++;

            Integer lastZoneId =
                    current.zoneIds.get(current.zoneIds.size() - 1);

            if (lastZoneId.equals(destinationZoneId)) {
                foundPaths.add(current);
                continue;
            }

            for (ZoneConnection connection :
                    graph.getOrDefault(
                            lastZoneId,
                            Collections.emptyList())) {

                Integer nextZoneId = connection.getToZone().getId();

                if (current.zoneIds.contains(nextZoneId)) {
                    continue;
                }

                List<Integer> nextPath =
                        new ArrayList<>(current.zoneIds);

                nextPath.add(nextZoneId);

                double nextCost = calculatePathCost(
                        nextPath,
                        latestSnapshots,
                        connections
                );

                queue.offer(new PathCandidate(nextPath, nextCost));
            }
        }

        return foundPaths;
    }

    private ZoneStepResult findAlternativeDestination(
            Integer startZoneId,
            Integer crowdedDestinationId,
            double crowdedDestinationRate,
            Map<Integer, List<ZoneConnection>> graph,
            List<ZoneConnection> connections,
            Map<Integer, SimulationSnapshot> latestSnapshots) {

        PriorityQueue<PathCandidate> queue = new PriorityQueue<>(
                Comparator.comparingDouble(path -> path.cost)
        );

        queue.offer(new PathCandidate(
                new ArrayList<>(Collections.singletonList(startZoneId)),
                0.0
        ));

        int explored = 0;
        final int MAX_EXPLORED = 10000;

        while (!queue.isEmpty() && explored < MAX_EXPLORED) {
            PathCandidate current = queue.poll();
            explored++;

            Integer lastZoneId =
                    current.zoneIds.get(current.zoneIds.size() - 1);

            if (!lastZoneId.equals(startZoneId)
                    && !lastZoneId.equals(crowdedDestinationId)) {

                Zone candidate = zoneRepository.findZoneById(lastZoneId);

                if (candidate != null) {
                    double rate =
                            (double) getOccupancy(candidate, latestSnapshots)
                                    / Math.max(candidate.getCapacity(), 1);

                    if (rate < crowdedDestinationRate && rate < 0.70) {
                        List<RouteResponseDTO.ZoneStep> path =
                                buildPathDetails(
                                        current.zoneIds,
                                        latestSnapshots
                                );

                        RouteResponseDTO.ZoneStep destinationStep =
                                path.isEmpty()
                                        ? null
                                        : path.get(path.size() - 1);

                        if (destinationStep != null) {
                            return new ZoneStepResult(
                                    destinationStep,
                                    path
                            );
                        }
                    }
                }
            }

            for (ZoneConnection connection :
                    graph.getOrDefault(
                            lastZoneId,
                            Collections.emptyList())) {

                Integer nextZoneId = connection.getToZone().getId();

                if (current.zoneIds.contains(nextZoneId)
                        || nextZoneId.equals(crowdedDestinationId)) {
                    continue;
                }

                List<Integer> nextPath =
                        new ArrayList<>(current.zoneIds);

                nextPath.add(nextZoneId);

                double nextCost = calculatePathCost(
                        nextPath,
                        latestSnapshots,
                        connections
                );

                queue.offer(new PathCandidate(nextPath, nextCost));
            }
        }

        return null;
    }

    private Map<Integer, SimulationSnapshot> getLatestSnapshots(
            List<SimulationSnapshot> snapshots) {

        Map<Integer, SimulationSnapshot> latest = new HashMap<>();

        for (SimulationSnapshot snapshot : snapshots) {
            if (snapshot.getZone() == null
                    || snapshot.getMinute() == null) {
                continue;
            }

            Integer zoneId = snapshot.getZone().getId();
            SimulationSnapshot existing = latest.get(zoneId);

            if (existing == null
                    || snapshot.getMinute() > existing.getMinute()) {
                latest.put(zoneId, snapshot);
            }
        }

        return latest;
    }

    private double calculatePathCost(
            List<Integer> zoneIds,
            Map<Integer, SimulationSnapshot> latestSnapshots,
            List<ZoneConnection> connections) {

        if (zoneIds.size() <= 1) {
            return 0.0;
        }

        double totalOccupancyRate = 0.0;
        double totalQueueRate = 0.0;

        for (Integer zoneId : zoneIds) {
            Zone zone = zoneRepository.findZoneById(zoneId);

            if (zone == null) {
                continue;
            }

            int capacity = Math.max(zone.getCapacity(), 1);
            int occupancy = getOccupancy(zone, latestSnapshots);

            double occupancyRate = Math.max(
                    0.0,
                    Math.min((double) occupancy / capacity, 1.0)
            );

            totalOccupancyRate += occupancyRate;

            SimulationSnapshot snapshot = latestSnapshots.get(zoneId);

            if (snapshot != null) {
                int queueLength = Math.max(snapshot.getQueueLength(), 0);

                double queueRate = Math.max(
                        0.0,
                        Math.min((double) queueLength / capacity, 1.0)
                );

                totalQueueRate += queueRate;
            }
        }

        double averageOccupancy =
                totalOccupancyRate / zoneIds.size();

        double averageQueue =
                totalQueueRate / zoneIds.size();

        double connectionCost = 0.0;

        for (int i = 0; i < zoneIds.size() - 1; i++) {
            Integer fromId = zoneIds.get(i);
            Integer toId = zoneIds.get(i + 1);

            for (ZoneConnection connection : connections) {
                if (connection.getFromZone() != null
                        && connection.getToZone() != null
                        && connection.getFromZone().getId().equals(fromId)
                        && connection.getToZone().getId().equals(toId)) {

                    int throughput = Math.max(
                            connection.getMaxVisitorsPerMinute(), 1
                    );

                    connectionCost += 10.0 / throughput;
                    break;
                }
            }
        }

        int edges = zoneIds.size() - 1;

        return (averageOccupancy * 100.0)
                + (averageQueue * 20.0)
                + (edges * 2.0)
                + connectionCost;
    }

    private List<RouteResponseDTO.ZoneStep> buildPathDetails(
            List<Integer> zoneIds,
            Map<Integer, SimulationSnapshot> latestSnapshots) {

        List<RouteResponseDTO.ZoneStep> details = new ArrayList<>();

        for (Integer zoneId : zoneIds) {
            Zone zone = zoneRepository.findZoneById(zoneId);

            if (zone == null) {
                continue;
            }

            int occupancy = getOccupancy(zone, latestSnapshots);
            int capacity = Math.max(zone.getCapacity(), 1);

            double rate = Math.max(
                    0.0,
                    Math.min((double) occupancy / capacity, 1.0)
            );

            details.add(new RouteResponseDTO.ZoneStep(
                    zone.getId(),
                    zone.getName(),
                    occupancy,
                    zone.getCapacity(),
                    Math.round(rate * 10000.0) / 10000.0,
                    getCongestionLevel(rate)
            ));
        }

        return details;
    }

    private int getOccupancy(
            Zone zone,
            Map<Integer, SimulationSnapshot> latestSnapshots) {

        SimulationSnapshot snapshot =
                latestSnapshots.get(zone.getId());

        return snapshot != null
                ? snapshot.getOccupancy()
                : zone.getInitialVisitors();
    }

    private String getPathCongestionLevel(
            List<RouteResponseDTO.ZoneStep> path) {

        double maxRate = path.stream()
                .mapToDouble(RouteResponseDTO.ZoneStep::getOccupancyRate)
                .max()
                .orElse(0.0);

        return getCongestionLevel(maxRate);
    }

    private String getCongestionLevel(double occupancyRate) {
        if (occupancyRate >= 0.90) {
            return "HIGH";
        } else if (occupancyRate >= 0.70) {
            return "MEDIUM";
        }

        return "LOW";
    }

    private static class PathCandidate {
        private final List<Integer> zoneIds;
        private final double cost;

        private PathCandidate(List<Integer> zoneIds, double cost) {
            this.zoneIds = zoneIds;
            this.cost = cost;
        }
    }

    private static class ZoneStepResult {
        private final RouteResponseDTO.ZoneStep destination;
        private final List<RouteResponseDTO.ZoneStep> path;

        private ZoneStepResult(
                RouteResponseDTO.ZoneStep destination,
                List<RouteResponseDTO.ZoneStep> path) {
            this.destination = destination;
            this.path = path;
        }
    }
}
