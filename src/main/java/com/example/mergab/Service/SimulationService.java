package com.example.mergab.Service;

import com.example.mergab.Api.ApiException;
import com.example.mergab.Model.HistoricalAttendance;
import com.example.mergab.Model.Simulation;
import com.example.mergab.Repository.HistoricalAttendanceRepository;
import com.example.mergab.Repository.SimulationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SimulationService {

    private final SimulationRepository simulationRepository;
    private final HistoricalAttendanceRepository historicalAttendanceRepository;



    public List<Simulation> getAllSimulations() {
        return simulationRepository.findAll();
    }

    public void addSimulation(Integer historicalAttendanceId, Simulation simulation) {
        HistoricalAttendance attendance = historicalAttendanceRepository.findHistoricalAttendanceById(historicalAttendanceId);
        if (attendance == null) {
            throw new ApiException("Historical attendance not found");
        }

        simulation.setHistoricalAttendance(attendance);
        // هذي يحددها النظام، مو المستخدم
        simulation.setStatus("PENDING");
        simulation.setStartDateTime(null);
        simulation.setEndDateTime(null);

        simulationRepository.save(simulation);
    }

    // التعديل مسموح بس قبل التشغيل
    public void updateSimulation(Integer id, Simulation newSimulation) {
        Simulation oldSimulation = simulationRepository.findSimulationById(id);
        if (oldSimulation == null) {
            throw new ApiException("Simulation not found");
        }
        if (!oldSimulation.getStatus().equals("PENDING")) {
            throw new ApiException("Only pending simulations can be updated");
        }

        oldSimulation.setDurationMinutes(newSimulation.getDurationMinutes());
        oldSimulation.setRandomSeed(newSimulation.getRandomSeed());

        simulationRepository.save(oldSimulation);
    }

    public void deleteSimulation(Integer id) {
        Simulation simulation = simulationRepository.findSimulationById(id);
        if (simulation == null) {
            throw new ApiException("Simulation not found");
        }
        if (simulation.getStatus().equals("RUNNING")) {
            throw new ApiException("Cannot delete a running simulation");
        }
        simulationRepository.delete(simulation);
    }
}
