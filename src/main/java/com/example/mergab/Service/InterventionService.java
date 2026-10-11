package com.example.mergab.Service;

import com.example.mergab.Api.ApiException;
import com.example.mergab.Model.Intervention;
import com.example.mergab.Model.Simulation;
import com.example.mergab.Model.Zone;
import com.example.mergab.Repository.InterventionRepository;
import com.example.mergab.Repository.SimulationRepository;
import com.example.mergab.Repository.ZoneRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class InterventionService {

    private final InterventionRepository interventionRepository;
    private final SimulationRepository simulationRepository;
    private final ZoneRepository zoneRepository;

    public List<Intervention> getAllInterventions() {
        return interventionRepository.findAll();
    }

    public void addIntervention(Integer simulationId, Integer sourceZoneId, Integer targetZoneId, Intervention intervention) {
        Simulation simulation = simulationRepository.findSimulationById(simulationId);
        if (simulation == null) {
            throw new ApiException("Simulation not found");
        }
        if (simulation.getStatus().equals("RUNNING")) {
            throw new ApiException("Cannot add intervention to a running simulation");
        }

        Zone sourceZone = zoneRepository.findZoneById(sourceZoneId);
        Zone targetZone = zoneRepository.findZoneById(targetZoneId);
        validateZonesAndMinute(simulation, sourceZone, targetZone, intervention.getStartMinute());

        intervention.setSimulation(simulation);
        intervention.setSourceZone(sourceZone);
        intervention.setTargetZone(targetZone);

        interventionRepository.save(intervention);
    }

    public void updateIntervention(Integer id, Integer sourceZoneId, Integer targetZoneId, Intervention newIntervention) {
        Intervention oldIntervention = interventionRepository.findInterventionById(id);
        if (oldIntervention == null) {
            throw new ApiException("Intervention not found");
        }

        Simulation simulation = oldIntervention.getSimulation();
        if (simulation.getStatus().equals("RUNNING")) {
            throw new ApiException("Cannot update intervention of a running simulation");
        }

        Zone sourceZone = zoneRepository.findZoneById(sourceZoneId);
        Zone targetZone = zoneRepository.findZoneById(targetZoneId);
        validateZonesAndMinute(simulation, sourceZone, targetZone, newIntervention.getStartMinute());

        oldIntervention.setSourceZone(sourceZone);
        oldIntervention.setTargetZone(targetZone);
        oldIntervention.setStartMinute(newIntervention.getStartMinute());
        oldIntervention.setVisitorsPerMinute(newIntervention.getVisitorsPerMinute());

        interventionRepository.save(oldIntervention);
    }

    public void deleteIntervention(Integer id) {
        Intervention intervention = interventionRepository.findInterventionById(id);
        if (intervention == null) {
            throw new ApiException("Intervention not found");
        }
        if (intervention.getSimulation().getStatus().equals("RUNNING")) {
            throw new ApiException("Cannot delete intervention of a running simulation");
        }
        interventionRepository.delete(intervention);
    }

    // Business rules مشتركة بين add و update
    private void validateZonesAndMinute(Simulation simulation, Zone sourceZone, Zone targetZone, Integer startMinute) {
        if (sourceZone == null) {
            throw new ApiException("Source zone not found");
        }
        if (targetZone == null) {
            throw new ApiException("Target zone not found");
        }
        if (sourceZone.getId().equals(targetZone.getId())) {
            throw new ApiException("Source zone and target zone must be different");
        }
        if (startMinute >= simulation.getDurationMinutes()) {
            throw new ApiException("Start minute must be less than simulation duration");
        }
    }
}
