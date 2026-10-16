package com.example.mergab.Controller;



import com.example.mergab.Api.ApiResponse;
import com.example.mergab.DTO.FacilityAccessPathDTO;
import com.example.mergab.Service.FacilityAccessPathService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/facility-access-path")
@RequiredArgsConstructor
public class FacilityAccessPathController {

    private final FacilityAccessPathService accessPathService;

    // Get all facility access paths
    @GetMapping("/get")
    public ResponseEntity<?> getAllAccessPaths() {
        return ResponseEntity.status(200)
                .body(accessPathService.getAllAccessPaths());
    }

    // Get access path by ID
    @GetMapping("/get-by-id/{id}")
    public ResponseEntity<?> getAccessPathById(
            @PathVariable Integer id) {

        return ResponseEntity.status(200)
                .body(accessPathService.getAccessPathById(id));
    }

    // Get all access paths for a facility
    @GetMapping("/get-by-facility/{facilityId}")
    public ResponseEntity<?> getPathsByFacility(
            @PathVariable Integer facilityId) {

        return ResponseEntity.status(200)
                .body(accessPathService.getPathsByFacility(facilityId));
    }

    // Get active access paths for routing
    @GetMapping("/get-active-by-facility/{facilityId}")
    public ResponseEntity<?> getActivePathsByFacility(
            @PathVariable Integer facilityId) {

        return ResponseEntity.status(200)
                .body(accessPathService.getActivePathsByFacility(facilityId));
    }

    // Add access path between walking path and facility
    @PostMapping("/add/{facilityId}/{walkPathId}")
    public ResponseEntity<?> addAccessPath(
            @PathVariable Integer facilityId,
            @PathVariable Integer walkPathId,
            @RequestBody @Valid FacilityAccessPathDTO dto) {

        accessPathService.addAccessPath(
                facilityId, walkPathId, dto);

        return ResponseEntity.status(201)
                .body(new ApiResponse("Facility access path added successfully"));
    }

    // Update access path
    @PutMapping("/update/{id}")
    public ResponseEntity<?> updateAccessPath(
            @PathVariable Integer id,
            @RequestBody @Valid FacilityAccessPathDTO dto) {

        accessPathService.updateAccessPath(id, dto);

        return ResponseEntity.status(200)
                .body(new ApiResponse("Facility access path updated successfully"));
    }

    // Enable or disable access path
    @PutMapping("/change-status/{id}")
    public ResponseEntity<?> changeAccessStatus(
            @PathVariable Integer id,
            @RequestParam Boolean active) {

        accessPathService.changeAccessStatus(id, active);

        return ResponseEntity.status(200)
                .body(new ApiResponse("Access path status updated successfully"));
    }

    // Delete access path
    @DeleteMapping("/delete/{id}")
    public ResponseEntity<?> deleteAccessPath(
            @PathVariable Integer id) {

        accessPathService.deleteAccessPath(id);

        return ResponseEntity.status(200)
                .body(new ApiResponse("Facility access path deleted successfully"));
    }
}
