package com.example.mergab.Controller;



import com.example.mergab.Api.ApiResponse;
import com.example.mergab.DTO.ZoneWalkPathDTO;
import com.example.mergab.Service.ZoneWalkPathService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/zone-walk-path")
@RequiredArgsConstructor
public class ZoneWalkPathController {

    private final ZoneWalkPathService walkPathService;

    // Get all walking paths
    @GetMapping("/get")
    public ResponseEntity<?> getAllPaths() {
        return ResponseEntity.status(200).body(walkPathService.getAllPaths());
    }

    // Get walking path by ID
    @GetMapping("/get-by-id/{id}")
    public ResponseEntity<?> getPathById(@PathVariable Integer id) {
        return ResponseEntity.status(200).body(walkPathService.getPathById(id));
    }

    // Get walking paths by zone
    @GetMapping("/get-by-zone/{zoneId}")
    public ResponseEntity<?> getPathsByZone(@PathVariable Integer zoneId) {
        return ResponseEntity.status(200).body(walkPathService.getPathsByZone(zoneId));
    }

    // Get active walking paths by zone
    @GetMapping("/get-active-by-zone/{zoneId}")
    public ResponseEntity<?> getActivePathsByZone(@PathVariable Integer zoneId) {
        return ResponseEntity.status(200).body(walkPathService.getActivePathsByZone(zoneId));
    }

    // Add walking path inside zone
    @PostMapping("/add/{zoneId}")
    public ResponseEntity<?> addPath(@PathVariable Integer zoneId, @RequestBody @Valid ZoneWalkPathDTO dto) {

        walkPathService.addPath(zoneId, dto);

        return ResponseEntity.status(201).body(new ApiResponse("Walking path added successfully"));
    }

    // Update walking path
    @PutMapping("/update/{id}")
    public ResponseEntity<?> updatePath(@PathVariable Integer id, @RequestBody @Valid ZoneWalkPathDTO dto) {

        walkPathService.updatePath(id, dto);

        return ResponseEntity.status(200).body(new ApiResponse("Walking path updated successfully"));
    }

    // Delete walking path
    @DeleteMapping("/delete/{id}")
    public ResponseEntity<?> deletePath(@PathVariable Integer id) {

        walkPathService.deletePath(id);

        return ResponseEntity.status(200).body(new ApiResponse("Walking path deleted successfully"));
    }
}
