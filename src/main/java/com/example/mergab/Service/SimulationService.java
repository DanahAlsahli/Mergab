package com.example.mergab.Service;

import com.example.mergab.Api.ApiException;
import com.example.mergab.Model.HistoricalAttendance;
import com.example.mergab.Model.Organizer;
import com.example.mergab.Model.Simulation;
import com.example.mergab.Repository.HistoricalAttendanceRepository;
import com.example.mergab.Repository.OrganizerRepository;
import com.example.mergab.Repository.SimulationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SimulationService {

    private final SimulationRepository simulationRepository;
    private final HistoricalAttendanceRepository historicalAttendanceRepository;
    private final OrganizerRepository organizerRepository;

    // كل المحاكاة (للأدمن)
    public List<Simulation> getAllSimulations() {
        return simulationRepository.findAll();
    }

    // محاكاة المنظم نفسه
    public List<Simulation> getMySimulations(Integer organizerId) {
        getOrganizer(organizerId);
        return simulationRepository.findSimulationsByOrganizer_Id(organizerId);
    }

    public void addSimulation(Integer organizerId, Integer historicalAttendanceId, Simulation simulation) {
        Organizer organizer = getOrganizer(organizerId);

        HistoricalAttendance attendance = historicalAttendanceRepository.findHistoricalAttendanceById(historicalAttendanceId);
        if (attendance == null) {
            throw new ApiException("Historical attendance not found");
        }

        simulation.setHistoricalAttendance(attendance);
        simulation.setOrganizer(organizer);
        // هذي يحددها النظام، مو المستخدم
        simulation.setStatus("PENDING");
        simulation.setStartDateTime(null);
        simulation.setEndDateTime(null);

        simulationRepository.save(simulation);
    }

    // التعديل مسموح بس قبل التشغيل
    public void updateSimulation(Integer organizerId, Integer id, Simulation newSimulation) {
        Simulation oldSimulation = getOwnedSimulation(organizerId, id);
        if (!oldSimulation.getStatus().equals("PENDING")) {
            throw new ApiException("Only pending simulations can be updated");
        }

        oldSimulation.setDurationMinutes(newSimulation.getDurationMinutes());
        oldSimulation.setRandomSeed(newSimulation.getRandomSeed());

        simulationRepository.save(oldSimulation);
    }

    public void deleteSimulation(Integer organizerId, Integer id) {
        Simulation simulation = getOwnedSimulation(organizerId, id);
        if (simulation.getStatus().equals("RUNNING")) {
            throw new ApiException("Cannot delete a running simulation");
        }
        simulationRepository.delete(simulation);
    }

    // ===== helpers =====

    private Organizer getOrganizer(Integer organizerId) {
        Organizer organizer = organizerRepository.findOrganizerById(organizerId);
        if (organizer == null) {
            throw new ApiException("Organizer not found");
        }
        return organizer;
    }

    // يتأكد إن المحاكاة موجودة وإنها تابعة لنفس المنظم
    private Simulation getOwnedSimulation(Integer organizerId, Integer simulationId) {
        Simulation simulation = simulationRepository.findSimulationById(simulationId);
        if (simulation == null) {
            throw new ApiException("Simulation not found");
        }
        if (!simulation.getOrganizer().getId().equals(organizerId)) {
            throw new ApiException("You are not allowed to modify this simulation");
        }
        return simulation;
    }
}
