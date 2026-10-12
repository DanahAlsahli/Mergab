package com.example.mergab.Controller;

import com.example.mergab.Api.ApiResponse;
import com.example.mergab.Model.Intervention;
import com.example.mergab.Service.InterventionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/intervention")
@RequiredArgsConstructor
public class InterventionController {

    private final InterventionService interventionService;

    @GetMapping("/get-all")
    public ResponseEntity<?> getAllInterventions() {
        return ResponseEntity.status(200).body(interventionService.getAllInterventions());
    }

    @PostMapping("/add/{simulationId}/{sourceZoneId}/{targetZoneId}")
    public ResponseEntity<?> addIntervention(@PathVariable Integer simulationId,
                                             @PathVariable Integer sourceZoneId,
                                             @PathVariable Integer targetZoneId,
                                             @RequestBody @Valid Intervention intervention) {
        interventionService.addIntervention(simulationId, sourceZoneId, targetZoneId, intervention);
        return ResponseEntity.status(200).body(new ApiResponse("Intervention added successfully"));
    }

    @PutMapping("/update/{id}/{sourceZoneId}/{targetZoneId}")
    public ResponseEntity<?> updateIntervention(@PathVariable Integer id,
                                                @PathVariable Integer sourceZoneId,
                                                @PathVariable Integer targetZoneId,
                                                @RequestBody @Valid Intervention intervention) {
        interventionService.updateIntervention(id, sourceZoneId, targetZoneId, intervention);
        return ResponseEntity.status(200).body(new ApiResponse("Intervention updated successfully"));
    }

    @DeleteMapping("/delete/{id}")
    public ResponseEntity<?> deleteIntervention(@PathVariable Integer id) {
        interventionService.deleteIntervention(id);
        return ResponseEntity.status(200).body(new ApiResponse("Intervention deleted successfully"));
    }
}
