package com.example.mergab.Service;



import com.example.mergab.Api.ApiException;
import com.example.mergab.DTO.ZoneWalkPathDTO;
import com.example.mergab.Model.Zone;
import com.example.mergab.Model.ZoneWalkPath;
import com.example.mergab.Repository.FacilityAccessPathRepository;
import com.example.mergab.Repository.ZoneRepository;
import com.example.mergab.Repository.ZoneWalkPathRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ZoneWalkPathService {

    private final ZoneWalkPathRepository pathRepository;
    private final ZoneRepository zoneRepository;
    private final FacilityAccessPathRepository accessPathRepository;

    private final JsonMapper jsonMapper = JsonMapper.builder().build();

    // Get all walking paths
    public List<ZoneWalkPath> getAllPaths() {
        return pathRepository.findAll();
    }

    // Get walking paths by zone
    public List<ZoneWalkPath> getPathsByZone(Integer zoneId) {
        getZone(zoneId);
        return pathRepository.findAllByZone_Id(zoneId);
    }

    // Get active walking paths by zone
    public List<ZoneWalkPath> getActivePathsByZone(Integer zoneId) {
        getZone(zoneId);
        return pathRepository.findAllByZone_IdAndActiveTrue(zoneId);
    }

    // Get walking path by ID
    public ZoneWalkPath getPathById(Integer id) {
        ZoneWalkPath path = pathRepository.findZoneWalkPathById(id);

        if (path == null) {
            throw new ApiException("Walking path not found");
        }

        return path;
    }

    // Add walking path inside zone
    public void addPath(Integer zoneId, ZoneWalkPathDTO dto) {

        Zone zone = getZone(zoneId);

        validatePath(dto.getPathPoints(), zone.getBoundaryPoints());

        ZoneWalkPath path = new ZoneWalkPath();

        path.setName(dto.getName());
        path.setPathPoints(dto.getPathPoints());
        path.setDirection(dto.getDirection());
        path.setActive(dto.getActive());
        path.setZone(zone);

        pathRepository.save(path);
    }

    // Update walking path
    public void updatePath(Integer id, ZoneWalkPathDTO dto) {

        ZoneWalkPath path = getPathById(id);

        validatePath(
                dto.getPathPoints(),
                path.getZone().getBoundaryPoints()
        );

        path.setName(dto.getName());
        path.setPathPoints(dto.getPathPoints());
        path.setDirection(dto.getDirection());
        path.setActive(dto.getActive());

        pathRepository.save(path);
    }

    // Delete walking path
    public void deletePath(Integer id) {

        ZoneWalkPath path = getPathById(id);

        // Prevent deleting a path used by facility entrances
        if (accessPathRepository.existsByZoneWalkPath_Id(id)) {
            throw new ApiException(
                    "Cannot delete a walking path linked to facility access paths"
            );
        }

        pathRepository.delete(path);
    }

    // Helper: Find zone
    private Zone getZone(Integer zoneId) {

        Zone zone = zoneRepository.findZoneById(zoneId);

        if (zone == null) {
            throw new ApiException("Zone not found");
        }

        return zone;
    }

    // Validate path coordinates and zone boundaries
    private void validatePath(String pathPoints, String boundaryPoints) {

        if (boundaryPoints == null || boundaryPoints.isBlank()) {
            throw new ApiException("Define zone boundaries first");
        }

        JsonNode path = parsePoints(pathPoints);
        JsonNode boundary = parsePoints(boundaryPoints);

        if (!path.isArray() || path.size() < 2) {
            throw new ApiException("Walking path requires at least 2 points");
        }

        if (!boundary.isArray() || boundary.size() < 3) {
            throw new ApiException("Invalid zone boundaries");
        }

        // Verify all boundary coordinates
        for (JsonNode point : boundary) {
            readPoint(point);
        }

        double[] previous = null;
        boolean hasMovement = false;

        for (JsonNode node : path) {

            double[] current = readPoint(node);

            if (!insideZone(current[0], current[1], boundary)) {
                throw new ApiException("Walking path must stay inside its zone");
            }

            if (previous != null) {

                double distance = Math.hypot(
                        current[0] - previous[0],
                        current[1] - previous[1]
                );

                if (distance > 0.000001) {
                    hasMovement = true;
                }

                // Check intermediate positions on each segment
                for (int step = 1; step < 20; step++) {

                    double t = step / 20.0;

                    double x = previous[0] + t * (current[0] - previous[0]);
                    double y = previous[1] + t * (current[1] - previous[1]);

                    if (!insideZone(x, y, boundary)) {
                        throw new ApiException(
                                "Walking path crosses outside zone boundaries"
                        );
                    }
                }
            }

            previous = current;
        }

        if (!hasMovement) {
            throw new ApiException("Walking path must have a valid length");
        }
    }

    // Parse JSON coordinates
    private JsonNode parsePoints(String json) {
        try {
            return jsonMapper.readTree(json);
        } catch (Exception e) {
            throw new ApiException("Invalid path points JSON");
        }
    }

    // Read [x,y] from JSON
    private double[] readPoint(JsonNode point) {

        if (!point.isArray() || point.size() != 2 ||
                !point.get(0).isNumber() || !point.get(1).isNumber()) {
            throw new ApiException("Invalid coordinate format");
        }

        double x = point.get(0).asDouble();
        double y = point.get(1).asDouble();

        if (!Double.isFinite(x) || !Double.isFinite(y)
                || x < 0 || x > 100 || y < 0 || y > 100) {
            throw new ApiException("Coordinates must be between 0 and 100");
        }

        return new double[]{x, y};
    }

    // Check whether a coordinate is inside the zone polygon
    private boolean insideZone(double x, double y, JsonNode polygon) {

        boolean inside = false;

        for (int i = 0, j = polygon.size() - 1;
             i < polygon.size(); j = i++) {

            double xi = polygon.get(i).get(0).asDouble();
            double yi = polygon.get(i).get(1).asDouble();
            double xj = polygon.get(j).get(0).asDouble();
            double yj = polygon.get(j).get(1).asDouble();

            // Accept points located exactly on the boundary
            double cross = (x - xi) * (yj - yi)
                    - (y - yi) * (xj - xi);

            if (Math.abs(cross) < 0.000001
                    && x >= Math.min(xi, xj) - 0.000001
                    && x <= Math.max(xi, xj) + 0.000001
                    && y >= Math.min(yi, yj) - 0.000001
                    && y <= Math.max(yi, yj) + 0.000001) {
                return true;
            }

            if ((yi > y) != (yj > y)
                    && x < (xj - xi) * (y - yi) / (yj - yi) + xi) {
                inside = !inside;
            }
        }

        return inside;
    }
}
