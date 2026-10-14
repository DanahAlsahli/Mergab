package com.example.mergab.Controller;



import com.example.mergab.Api.ApiResponse;
import com.example.mergab.DTO.FacilityDTO;
import com.example.mergab.Service.FacilityService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/facility")
@RequiredArgsConstructor
public class FacilityController {

    private final FacilityService facilityService;


    // Get all facilities in the Expo
    @GetMapping("/get")
    public ResponseEntity<?> getAllFacilities() {
        return ResponseEntity.status(200).body(facilityService.getAllFacilities());
    }


    // Get a specific facility by ID
    @GetMapping("/get-by-id/{id}")
    public ResponseEntity<?> getFacilityById(@PathVariable Integer id) {
        return ResponseEntity.status(200).body(facilityService.getFacilityById(id));
    }


    // Get all facilities belonging to a specific zone
    @GetMapping("/get-by-zone/{zoneId}")
    public ResponseEntity<?> getFacilitiesByZone(@PathVariable Integer zoneId) {
        return ResponseEntity.status(200).body(facilityService.getFacilitiesByZone(zoneId));
    }


    // Add a new facility to an existing zone
    @PostMapping("/add/{zoneId}")
    public ResponseEntity<?> addFacility(@PathVariable Integer zoneId, @RequestBody @Valid FacilityDTO dto) {

        facilityService.addFacility(zoneId, dto);

        return ResponseEntity.status(200).body(new ApiResponse("Facility added successfully"));
    }


    // Update facility information and coordinates
    @PutMapping("/update/{id}")
    public ResponseEntity<?> updateFacility(@PathVariable Integer id, @RequestBody @Valid FacilityDTO dto) {

        facilityService.updateFacility(id, dto);

        return ResponseEntity.status(200).body(new ApiResponse("Facility updated successfully"));
    }


    // Delete a facility
    @DeleteMapping("/delete/{id}")
    public ResponseEntity<?> deleteFacility(@PathVariable Integer id) {

        facilityService.deleteFacility(id);

        return ResponseEntity.status(200).body(new ApiResponse("Facility deleted successfully"));
    }


    // Find the nearest facility based on GPS location, type, and gender
    @GetMapping("/nearest")
    public ResponseEntity<?> getNearestFacility(@RequestParam Double latitude, @RequestParam Double longitude, @RequestParam String type, @RequestParam String gender) {

        return ResponseEntity.status(200).body(facilityService.getNearestFacility(latitude, longitude, type, gender));
    }

    // Find the nearest facility on the virtual Expo map
    @GetMapping("/nearest-map")
    public ResponseEntity<?> getNearestMapFacility(@RequestParam Double mapX, @RequestParam Double mapY, @RequestParam String type, @RequestParam String gender) {

        return ResponseEntity.status(200).body(facilityService.getNearestMapFacility(mapX, mapY, type, gender));
    }
}
