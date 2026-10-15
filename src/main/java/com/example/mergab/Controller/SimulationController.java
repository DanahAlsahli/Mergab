package com.example.mergab.Controller;

import com.example.mergab.Api.ApiResponse;
import com.example.mergab.Model.Simulation;
import com.example.mergab.Model.SimulationSnapshot;
import java.util.List;
import com.example.mergab.Service.SimulationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.example.mergab.Model.User;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

@RestController
@RequestMapping("/api/v1/simulation")
@RequiredArgsConstructor
public class SimulationController {

    private final SimulationService simulationService;

    @GetMapping("/get-all")
    public ResponseEntity<?> getAllSimulations() {
        return ResponseEntity.status(200).body(simulationService.getAllSimulations());
    }

    @GetMapping("/get-by-organizer/{organizerId}")
    public ResponseEntity<?> getSimulationsByOrganizer(@PathVariable Integer organizerId) {
        return ResponseEntity.status(200).body(simulationService.getSimulationsByOrganizer(organizerId));
    }

    // Add a simulation for the authenticated organizer
    @PostMapping("/add/{historicalAttendanceId}")
    public ResponseEntity<?> addSimulation(@AuthenticationPrincipal User user, @PathVariable Integer historicalAttendanceId, @RequestBody @Valid Simulation simulation) {

        simulationService.addSimulation(user.getId(), historicalAttendanceId, simulation);

        return ResponseEntity.status(201).body(new ApiResponse("Simulation added successfully"));
    }

    // run the simulation
    @PostMapping("/run/{id}")
    public ResponseEntity<?> runSimulation(@PathVariable Integer id) {
        List<SimulationSnapshot> snapshots = simulationService.runSimulation(id);
        return ResponseEntity.status(200).body(snapshots);
    }

    @PutMapping("/update/{id}")
    public ResponseEntity<?> updateSimulation(@PathVariable Integer id, @RequestBody @Valid Simulation simulation) {
        simulationService.updateSimulation(id, simulation);
        return ResponseEntity.status(200).body(new ApiResponse("Simulation updated successfully"));
    }

    @DeleteMapping("/delete/{id}")
    public ResponseEntity<?> deleteSimulation(@PathVariable Integer id) {
        simulationService.deleteSimulation(id);
        return ResponseEntity.status(200).body(new ApiResponse("Simulation deleted successfully"));
    }

    @GetMapping("/get-by-id/{id}")
    public ResponseEntity<?> getSimulationById(@PathVariable Integer id) {
        return ResponseEntity.status(200).body(simulationService.getSimulationById(id));
    }

    @GetMapping("/get-by-status/{status}")
    public ResponseEntity<?> getSimulationsByStatus(@PathVariable String status) {
        return ResponseEntity.status(200).body(simulationService.getSimulationsByStatus(status));
    }
}
