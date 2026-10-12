package com.example.mergab.Controller;

import com.example.mergab.Api.ApiResponse;
import com.example.mergab.Model.Simulation;
import com.example.mergab.Service.SimulationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/simulation")
@RequiredArgsConstructor
public class SimulationController {

    private final SimulationService simulationService;

    @GetMapping("/get-all")
    public ResponseEntity<?> getAllSimulations() {
        return ResponseEntity.status(200).body(simulationService.getAllSimulations());
    }

    @PostMapping("/add/{historicalAttendanceId}")
    public ResponseEntity<?> addSimulation(@PathVariable Integer historicalAttendanceId, @RequestBody @Valid Simulation simulation) {
        simulationService.addSimulation(historicalAttendanceId, simulation);
        return ResponseEntity.status(200).body(new ApiResponse("Simulation added successfully"));
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

}
