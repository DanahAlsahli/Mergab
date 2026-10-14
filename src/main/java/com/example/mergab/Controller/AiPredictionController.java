package com.example.mergab.Controller;
import com.example.mergab.DTO.AiPredictionResponseDTO;
import com.example.mergab.Model.SimulationSnapshot;
import com.example.mergab.Model.Zone;
import com.example.mergab.Repository.ZoneRepository;
import com.example.mergab.Service.AiPredictionService;
import com.example.mergab.Service.SimulationSnapshotService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import com.example.mergab.Service.AiAnalysisService;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/ai")
@RequiredArgsConstructor
public class AiPredictionController {

    private final AiPredictionService aiPredictionService;
    private final SimulationSnapshotService simulationSnapshotService;
    private final ZoneRepository zoneRepository;
    private final AiAnalysisService aiAnalysisService;

    @GetMapping("/predict/{simulationId}/{zoneId}")
    public AiPredictionResponseDTO predict(
            @PathVariable Integer simulationId,
            @PathVariable Integer zoneId) {

        Zone zone = zoneRepository.findZoneById(zoneId);

        if (zone == null) {
            throw new com.example.mergab.Api.ApiException(
                    "Zone not found");
        }

        List<SimulationSnapshot> allSnapshots =
                simulationSnapshotService
                        .getSnapshotsBySimulationAndZone(
                                simulationId, zoneId);

        List<SimulationSnapshot> recentSnapshots =
                allSnapshots.size() <= 3
                        ? allSnapshots
                        : allSnapshots.subList(
                        allSnapshots.size() - 3,
                        allSnapshots.size());

        if (recentSnapshots.isEmpty()) {
            throw new com.example.mergab.Api.ApiException(
                    "No snapshots found for prediction");
        }

        var result = aiPredictionService.predict(
                simulationId,
                zoneId,
                zone.getCapacity(),
                recentSnapshots);

        return new AiPredictionResponseDTO(
                ((Number) result.get("zoneId")).intValue(),
                ((Number) result.get("predictedOccupancy")).intValue(),
                result.get("predictedCongestionLevel").toString(),
                ((Number) result.get("horizonMinutes")).intValue());
    }


    @PostMapping("/analyze/{simulationId}/{zoneId}")
    public Map<String, Object> analyze(
            @PathVariable Integer simulationId,
            @PathVariable Integer zoneId) {

        Zone zone = zoneRepository.findZoneById(zoneId);

        if (zone == null) {
            throw new com.example.mergab.Api.ApiException(
                    "Zone not found");
        }

        List<SimulationSnapshot> allSnapshots =
                simulationSnapshotService
                        .getSnapshotsBySimulationAndZone(
                                simulationId, zoneId);

        List<SimulationSnapshot> recentSnapshots =
                allSnapshots.size() <= 3
                        ? allSnapshots
                        : allSnapshots.subList(
                        allSnapshots.size() - 3,
                        allSnapshots.size());

        if (recentSnapshots.isEmpty()) {
            throw new com.example.mergab.Api.ApiException(
                    "No snapshots found for analysis");
        }

        Map<String, Object> prediction = aiPredictionService.predict(
                simulationId,
                zoneId,
                zone.getCapacity(),
                recentSnapshots);

        Integer predictedOccupancy =
                ((Number) prediction.get("predictedOccupancy")).intValue();

        String congestionLevel =
                prediction.get("predictedCongestionLevel").toString();

        Integer horizonMinutes =
                ((Number) prediction.get("horizonMinutes")).intValue();

        return aiAnalysisService.analyze(
                simulationId,
                zoneId,
                zone.getCapacity(),
                predictedOccupancy,
                congestionLevel,
                horizonMinutes);
    }

}
