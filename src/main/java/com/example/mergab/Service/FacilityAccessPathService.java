package com.example.mergab.Service;



import com.example.mergab.Api.ApiException;
import com.example.mergab.DTO.FacilityAccessPathDTO;
import com.example.mergab.Model.Facility;
import com.example.mergab.Model.FacilityAccessPath;
import com.example.mergab.Model.ZoneWalkPath;
import com.example.mergab.Repository.FacilityAccessPathRepository;
import com.example.mergab.Repository.FacilityRepository;
import com.example.mergab.Repository.ZoneWalkPathRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class FacilityAccessPathService {

    private final FacilityAccessPathRepository accessPathRepository;
    private final FacilityRepository facilityRepository;
    private final ZoneWalkPathRepository walkPathRepository;

    private final JsonMapper jsonMapper = JsonMapper.builder().build();

    // Allowed coordinate difference on the virtual map
    private static final double TOLERANCE = 0.5;

    // Get all access paths
    public List<FacilityAccessPath> getAllAccessPaths() {
        return accessPathRepository.findAll();
    }

    // Get access path by ID
    public FacilityAccessPath getAccessPathById(Integer id) {

        FacilityAccessPath path =
                accessPathRepository.findFacilityAccessPathById(id);

        if (path == null) {
            throw new ApiException("Facility access path not found");
        }

        return path;
    }

    // Get all access paths belonging to a facility
    public List<FacilityAccessPath> getPathsByFacility(Integer facilityId) {

        getFacility(facilityId);

        return accessPathRepository.findAllByFacility_Id(facilityId);
    }

    // Get usable access paths for routing
    public List<FacilityAccessPath> getActivePathsByFacility(Integer facilityId) {

        getFacility(facilityId);

        return accessPathRepository
                .findAllByFacility_IdAndActiveTrue(facilityId)
                .stream()
                .filter(path -> Boolean.TRUE.equals(
                        path.getZoneWalkPath().getActive()))
                .toList();
    }

    // Add an access path from a walking path to a facility
    public void addAccessPath(
            Integer facilityId,
            Integer walkPathId,
            FacilityAccessPathDTO dto) {

        Facility facility = getFacility(facilityId);
        ZoneWalkPath walkPath = getWalkPath(walkPathId);

        validateAccessPath(facility, walkPath, dto);

        FacilityAccessPath accessPath = new FacilityAccessPath();

        accessPath.setPathPoints(dto.getPathPoints());
        accessPath.setDirection(dto.getDirection());
        accessPath.setActive(dto.getActive());
        accessPath.setFacility(facility);
        accessPath.setZoneWalkPath(walkPath);

        accessPathRepository.save(accessPath);
    }

    // Update an existing facility access path
    public void updateAccessPath(Integer id, FacilityAccessPathDTO dto) {

        FacilityAccessPath accessPath = getAccessPathById(id);

        validateAccessPath(
                accessPath.getFacility(),
                accessPath.getZoneWalkPath(),
                dto
        );

        accessPath.setPathPoints(dto.getPathPoints());
        accessPath.setDirection(dto.getDirection());
        accessPath.setActive(dto.getActive());

        accessPathRepository.save(accessPath);
    }

    // Enable or disable an access path
    public void changeAccessStatus(Integer id, Boolean active) {

        if (active == null) {
            throw new ApiException("Active status is required");
        }

        FacilityAccessPath path = getAccessPathById(id);

        path.setActive(active);
        accessPathRepository.save(path);
    }

    // Delete an access path without deleting its facility
    public void deleteAccessPath(Integer id) {

        FacilityAccessPath path = getAccessPathById(id);

        accessPathRepository.delete(path);
    }

    // Find facility
    private Facility getFacility(Integer id) {

        Facility facility = facilityRepository.findFacilityById(id);

        if (facility == null) {
            throw new ApiException("Facility not found");
        }

        return facility;
    }

    // Find walking path
    private ZoneWalkPath getWalkPath(Integer id) {

        ZoneWalkPath path = walkPathRepository.findZoneWalkPathById(id);

        if (path == null) {
            throw new ApiException("Walking path not found");
        }

        return path;
    }

    // Validate facility access path
    private void validateAccessPath(
            Facility facility,
            ZoneWalkPath walkPath,
            FacilityAccessPathDTO dto) {

        // Facility and walking path must belong to same zone
        if (!facility.getZone().getId()
                .equals(walkPath.getZone().getId())) {

            throw new ApiException(
                    "Facility and walking path must be in the same zone");
        }

        if (facility.getMapX() == null || facility.getMapY() == null) {
            throw new ApiException(
                    "Facility must have a location on the Expo map");
        }

        if (!List.of("BOTH", "TO_FACILITY", "FROM_FACILITY")
                .contains(dto.getDirection())) {

            throw new ApiException("Invalid access direction");
        }

        if (dto.getActive() == null) {
            throw new ApiException("Active status is required");
        }

        List<double[]> accessPoints = parsePoints(dto.getPathPoints(), 2);
        List<double[]> walkingPoints = parsePoints(walkPath.getPathPoints(), 2);
        List<double[]> boundary = parsePoints(
                facility.getZone().getBoundaryPoints(), 3);

        double[] start = accessPoints.get(0);
        double[] end = accessPoints.get(accessPoints.size() - 1);

        double[] facilityPoint = {
                facility.getMapX(),
                facility.getMapY()
        };

        // Access path must start on or very near the walking path
        if (distanceToPath(start, walkingPoints) > TOLERANCE) {
            throw new ApiException(
                    "Access path must start near its walking path");
        }

        // Access path must end at the facility entrance
        if (distance(end, facilityPoint) > TOLERANCE) {
            throw new ApiException(
                    "Access path must end at the facility location");
        }

        if (!insideZone(facilityPoint[0], facilityPoint[1], boundary)) {
            throw new ApiException(
                    "Facility location must be inside its zone");
        }

        boolean hasMovement = false;

        for (int i = 0; i < accessPoints.size(); i++) {

            double[] current = accessPoints.get(i);

            if (!insideZone(current[0], current[1], boundary)) {
                throw new ApiException(
                        "Access path must stay inside its zone");
            }

            if (i == 0) {
                continue;
            }

            double[] previous = accessPoints.get(i - 1);

            if (distance(previous, current) > 0.000001) {
                hasMovement = true;
            }

            // Sample intermediate points along each segment
            for (int step = 1; step < 20; step++) {

                double t = step / 20.0;

                double x = previous[0] +
                        t * (current[0] - previous[0]);

                double y = previous[1] +
                        t * (current[1] - previous[1]);

                if (!insideZone(x, y, boundary)) {
                    throw new ApiException(
                            "Access path crosses zone boundaries");
                }
            }
        }

        if (!hasMovement) {
            throw new ApiException("Access path must have a valid length");
        }
    }

    // Parse and validate coordinates
    private List<double[]> parsePoints(String json, int minimum) {

        JsonNode root;

        try {
            root = jsonMapper.readTree(json);
        } catch (Exception e) {
            throw new ApiException("Invalid coordinates JSON");
        }

        if (root == null || !root.isArray() || root.size() < minimum) {
            throw new ApiException("Invalid number of coordinates");
        }

        List<double[]> points = new ArrayList<>();

        for (JsonNode node : root) {

            if (!node.isArray() || node.size() != 2 ||
                    !node.get(0).isNumber() ||
                    !node.get(1).isNumber()) {

                throw new ApiException("Invalid coordinate format");
            }

            double x = node.get(0).asDouble();
            double y = node.get(1).asDouble();

            if (!Double.isFinite(x) || !Double.isFinite(y)
                    || x < 0 || x > 100
                    || y < 0 || y > 100) {

                throw new ApiException(
                        "Coordinates must be between 0 and 100");
            }

            points.add(new double[]{x, y});
        }

        return points;
    }

    // Calculate distance between two points
    private double distance(double[] a, double[] b) {
        return Math.hypot(a[0] - b[0], a[1] - b[1]);
    }

    // Calculate shortest distance from a point to a walking path
    private double distanceToPath(
            double[] point,
            List<double[]> path) {

        double minimum = Double.MAX_VALUE;

        for (int i = 1; i < path.size(); i++) {

            double[] a = path.get(i - 1);
            double[] b = path.get(i);

            double dx = b[0] - a[0];
            double dy = b[1] - a[1];

            double lengthSquared = dx * dx + dy * dy;

            double t = 0;

            if (lengthSquared > 0) {
                t = ((point[0] - a[0]) * dx +
                        (point[1] - a[1]) * dy) / lengthSquared;
            }

            t = Math.max(0, Math.min(1, t));

            double[] nearest = {
                    a[0] + t * dx,
                    a[1] + t * dy
            };

            minimum = Math.min(minimum, distance(point, nearest));
        }

        return minimum;
    }

    // Check if a point is inside the zone polygon
    private boolean insideZone(
            double x,
            double y,
            List<double[]> polygon) {

        boolean inside = false;

        for (int i = 0, j = polygon.size() - 1;
             i < polygon.size(); j = i++) {

            double[] a = polygon.get(i);
            double[] b = polygon.get(j);

            double cross = (x - a[0]) * (b[1] - a[1])
                    - (y - a[1]) * (b[0] - a[0]);

            if (Math.abs(cross) < 0.000001
                    && x >= Math.min(a[0], b[0]) - 0.000001
                    && x <= Math.max(a[0], b[0]) + 0.000001
                    && y >= Math.min(a[1], b[1]) - 0.000001
                    && y <= Math.max(a[1], b[1]) + 0.000001) {
                return true;
            }

            if ((a[1] > y) != (b[1] > y)
                    && x < (b[0] - a[0]) *
                    (y - a[1]) / (b[1] - a[1]) + a[0]) {

                inside = !inside;
            }
        }

        return inside;
    }
}
