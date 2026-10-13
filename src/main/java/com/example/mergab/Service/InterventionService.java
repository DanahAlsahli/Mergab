package com.example.mergab.Service;

import com.example.mergab.Api.ApiException;
import com.example.mergab.Model.Intervention;
import com.example.mergab.Model.Simulation;
import com.example.mergab.Model.Zone;
import com.example.mergab.Repository.InterventionRepository;
import com.example.mergab.Repository.SimulationRepository;
import com.example.mergab.Repository.ZoneConnectionRepository;
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
    private final ZoneConnectionRepository zoneConnectionRepository;

    public List<Intervention> getAllInterventions() {
        return interventionRepository.findAll();
    }

    public void addIntervention(Integer simulationId, Integer sourceZoneId, Integer targetZoneId, Intervention intervention) {
        Simulation simulation = simulationRepository.findSimulationById(simulationId);
        if (simulation == null) {
            throw new ApiException("Simulation not found");
        }

        // Allow interventions only before simulation starts
        if (!simulation.getStatus().equals("PENDING")) {
            throw new ApiException("Interventions can only be added to pending simulations");
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
        // Prevent modifying interventions after simulation starts
        if (!simulation.getStatus().equals("PENDING")) {
            throw new ApiException("Only pending simulation interventions can be updated");
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

        // Prevent deleting interventions after simulation starts
        if (!intervention.getSimulation().getStatus().equals("PENDING")) {
            throw new ApiException("Only pending simulation interventions can be deleted");
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
        // Check if a directed connection exists
        if (!zoneConnectionRepository.existsByFromZone_IdAndToZone_Id(sourceZone.getId(), targetZone.getId())) {
            throw new ApiException("No connection exists between source and target zones");
        }
        if (startMinute >= simulation.getDurationMinutes()) {
            throw new ApiException("Start minute must be less than simulation duration");
        }
    }


    // Get intervention by ID
    public Intervention getInterventionById(Integer id) {
        Intervention intervention = interventionRepository.findInterventionById(id);

        // Check if intervention exists
        if (intervention == null) {
            throw new ApiException("Intervention not found");
        }

        return intervention;
    }

    // Get interventions for a simulation
    public List<Intervention> getInterventionsBySimulation(Integer simulationId) {

        // Check if simulation exists
        if (!simulationRepository.existsById(simulationId)) {
            throw new ApiException("Simulation not found");
        }

        return interventionRepository.findBySimulation_Id(simulationId);
    }
}
