package com.example.mergab.Controller;


import com.example.mergab.Api.ApiResponse;
import com.example.mergab.Model.SimulationSnapshot;
import com.example.mergab.Service.SimulationSnapshotService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/simulation-snapshot")
@RequiredArgsConstructor
public class SimulationSnapshotController {


    private final SimulationSnapshotService simulationSnapshotService;

    // Get all snapshots
    @GetMapping("/get")
    public ResponseEntity<?> getSimulationSnapshots() {
        return ResponseEntity.status(200).body(simulationSnapshotService.getSimulationSnapshots());
    }


    // Add snapshot
    @PostMapping("/add/{simulationId}/{zoneId}")
    public ResponseEntity<?> addSimulationSnapshot(@PathVariable Integer simulationId, @PathVariable Integer zoneId, @RequestBody @Valid SimulationSnapshot snapshot) {
        simulationSnapshotService.addSimulationSnapshot(simulationId, zoneId, snapshot);
        return ResponseEntity.status(201).body(new ApiResponse("Simulation snapshot added successfully"));
    }

    // Update snapshot
    @PutMapping("/update/{id}")
    public ResponseEntity<?> updateSimulationSnapshot(@PathVariable Integer id, @RequestBody @Valid SimulationSnapshot snapshot) {
        simulationSnapshotService.updateSimulationSnapshot(id, snapshot);
        return ResponseEntity.status(200).body(new ApiResponse("Simulation snapshot updated successfully"));
    }

    // Change snapshot zone
    @PutMapping("/change-zone/{snapshotId}/{zoneId}")
    public ResponseEntity<?> changeSnapshotZone(@PathVariable Integer snapshotId, @PathVariable Integer zoneId) {
        simulationSnapshotService.changeSnapshotZone(snapshotId, zoneId);
        return ResponseEntity.status(200).body(new ApiResponse("Snapshot zone changed successfully"));
    }

    // Delete snapshot
    @DeleteMapping("/delete/{id}")
    public ResponseEntity<?> deleteSimulationSnapshot(@PathVariable Integer id) {
        simulationSnapshotService.deleteSimulationSnapshot(id);
        return ResponseEntity.status(200).body(new ApiResponse("Simulation snapshot deleted successfully"));
    }

    // Get snapshot by ID
    @GetMapping("/get-by-id/{id}")
    public ResponseEntity<?> getSnapshotById(@PathVariable Integer id) {
        return ResponseEntity.status(200).body(simulationSnapshotService.getSnapshotById(id));
    }

    // Get snapshots by zone
    @GetMapping("/get-by-zone/{zoneId}")
    public ResponseEntity<?> getSnapshotsByZone(@PathVariable Integer zoneId) {
        return ResponseEntity.status(200).body(simulationSnapshotService.getSnapshotsByZone(zoneId));
    }

    // Get zone snapshots ordered by minute
    @GetMapping("/get-by-zone-ordered/{zoneId}")
    public ResponseEntity<?> getSnapshotsByZoneOrdered(@PathVariable Integer zoneId) {
        return ResponseEntity.status(200).body(simulationSnapshotService.getSnapshotsByZoneOrdered(zoneId));
    }

    // Get snapshots by simulation
    @GetMapping("/get-by-simulation/{simulationId}")
    public ResponseEntity<?> getSnapshotsBySimulation(@PathVariable Integer simulationId) {
        return ResponseEntity.status(200).body(simulationSnapshotService.getSnapshotsBySimulation(simulationId));
    }

    // Get zone snapshots in a simulation
    @GetMapping("/get-by-simulation-and-zone/{simulationId}/{zoneId}")
    public ResponseEntity<?> getSnapshotsBySimulationAndZone(@PathVariable Integer simulationId, @PathVariable Integer zoneId) {
        return ResponseEntity.status(200).body(simulationSnapshotService.getSnapshotsBySimulationAndZone(simulationId, zoneId));
    }
}
