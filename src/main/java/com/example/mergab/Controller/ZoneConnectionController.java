package com.example.mergab.Controller;


import com.example.mergab.Api.ApiResponse;
import com.example.mergab.Model.ZoneConnection;
import com.example.mergab.Service.ZoneConnectionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/zone-connection")
@RequiredArgsConstructor
public class ZoneConnectionController {


    private final ZoneConnectionService zoneConnectionService;

    @GetMapping("/get")
    public ResponseEntity<?> getZoneConnections() {
        return ResponseEntity.status(200).body(zoneConnectionService.getZoneConnections());
    }

    // Add zone connection
    @PostMapping("/add/{fromZoneId}/{toZoneId}")
    public ResponseEntity<?> addZoneConnection(@PathVariable Integer fromZoneId, @PathVariable Integer toZoneId, @RequestBody @Valid ZoneConnection zoneConnection) {
        zoneConnectionService.addZoneConnection(fromZoneId, toZoneId, zoneConnection);
        return ResponseEntity.status(201).body(new ApiResponse("Zone connection added successfully"));
    }

    // Update connection capacity
    @PutMapping("/update/{id}")
    public ResponseEntity<?> updateZoneConnection(@PathVariable Integer id, @RequestBody @Valid ZoneConnection zoneConnection) {
        zoneConnectionService.updateZoneConnection(id, zoneConnection);
        return ResponseEntity.status(200).body(new ApiResponse("Zone connection updated successfully"));
    }

    // Change connected zones
    @PutMapping("/change-zones/{id}/{fromZoneId}/{toZoneId}")
    public ResponseEntity<?> changeConnectionZones(@PathVariable Integer id, @PathVariable Integer fromZoneId, @PathVariable Integer toZoneId) {
        zoneConnectionService.changeConnectionZones(id, fromZoneId, toZoneId);
        return ResponseEntity.status(200).body(new ApiResponse("Connection zones changed successfully"));
    }

    // Delete zone connection
    @DeleteMapping("/delete/{id}")
    public ResponseEntity<?> deleteZoneConnection(@PathVariable Integer id) {
        zoneConnectionService.deleteZoneConnection(id);
        return ResponseEntity.status(200).body(new ApiResponse("Zone connection deleted successfully"));
    }


}
