package com.example.mergab.Controller;

import com.example.mergab.Api.ApiResponse;
import com.example.mergab.Model.Simulation;
import com.example.mergab.Service.SimulationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

// TODO (Security): بدّلي {organizerId} بـ @AuthenticationPrincipal User user
@RestController
@RequestMapping("/api/v1/simulation")
@RequiredArgsConstructor
public class SimulationController {

    private final SimulationService simulationService;

    @GetMapping("/get-all")
    public ResponseEntity<?> getAllSimulations() {
        return ResponseEntity.status(200).body(simulationService.getAllSimulations());
    }

    @GetMapping("/my-simulations/{organizerId}")
    public ResponseEntity<?> getMySimulations(@PathVariable Integer organizerId) {
        return ResponseEntity.status(200).body(simulationService.getMySimulations(organizerId));
    }

    @PostMapping("/add/{organizerId}/{historicalAttendanceId}")
    public ResponseEntity<?> addSimulation(@PathVariable Integer organizerId, @PathVariable Integer historicalAttendanceId, @RequestBody @Valid Simulation simulation) {
        simulationService.addSimulation(organizerId, historicalAttendanceId, simulation);
        return ResponseEntity.status(201).body(new ApiResponse("Simulation added successfully"));
    }

    @PutMapping("/update/{organizerId}/{id}")
    public ResponseEntity<?> updateSimulation(@PathVariable Integer organizerId, @PathVariable Integer id, @RequestBody @Valid Simulation simulation) {
        simulationService.updateSimulation(organizerId, id, simulation);
        return ResponseEntity.status(200).body(new ApiResponse("Simulation updated successfully"));
    }

    @DeleteMapping("/delete/{organizerId}/{id}")
    public ResponseEntity<?> deleteSimulation(@PathVariable Integer organizerId, @PathVariable Integer id) {
        simulationService.deleteSimulation(organizerId, id);
        return ResponseEntity.status(200).body(new ApiResponse("Simulation deleted successfully"));
    }
}
