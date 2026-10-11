package com.example.mergab.Controller;


import com.example.mergab.Api.ApiResponse;
import com.example.mergab.Model.Zone;
import com.example.mergab.Service.ZoneService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/zone")
@RequiredArgsConstructor
public class ZoneController {

    private final ZoneService zoneService;

    @GetMapping("/get")
    public ResponseEntity<?>getZones(){
        return ResponseEntity.status(200).body(zoneService.getZones());
    }

    @PostMapping("/add")
    public ResponseEntity<?> addZone(@RequestBody @Valid Zone zone) {
        zoneService.addZone(zone);
        return ResponseEntity.status(201).body(new ApiResponse("Zone added successfully"));
    }

    @PutMapping("/update/{zoneId}")
    public ResponseEntity<?> updateZone(@PathVariable Integer zoneId, @RequestBody @Valid Zone zone) {
        zoneService.updateZone(zoneId, zone);
        return ResponseEntity.status(200).body(new ApiResponse("Zone updated successfully"));
    }

    @DeleteMapping("/delete/{zoneId}")
    public ResponseEntity<?> deleteZone(@PathVariable Integer zoneId) {
        zoneService.deleteZone(zoneId);
        return ResponseEntity.status(200).body(new ApiResponse("Zone deleted successfully"));
    }

    @GetMapping("/get-zone-by-name/{name}")
    public ResponseEntity<?> getZoneByName(@PathVariable String name) {
        return ResponseEntity.status(200).body(zoneService.getZoneByName(name));
    }
}
