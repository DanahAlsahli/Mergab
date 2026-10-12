package com.example.mergab.Controller;

import com.example.mergab.Api.ApiResponse;
import com.example.mergab.DTO.OrganizerDTO;
import com.example.mergab.Service.OrganizerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/organizer")
@RequiredArgsConstructor
public class OrganizerController {

    private final OrganizerService organizerService;

    @GetMapping("/get-all")
    public ResponseEntity<?> getAllOrganizers() {
        return ResponseEntity.status(200).body(organizerService.getAllOrganizers());
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody @Valid OrganizerDTO dto) {
        organizerService.register(dto);
        return ResponseEntity.status(201).body(new ApiResponse("Organizer registered successfully"));
    }

    @PutMapping("/update/{id}")
    public ResponseEntity<?> updateOrganizer(@PathVariable Integer id, @RequestBody @Valid OrganizerDTO dto) {
        organizerService.updateOrganizer(id, dto);
        return ResponseEntity.status(200).body(new ApiResponse("Organizer updated successfully"));
    }

    @DeleteMapping("/delete/{id}")
    public ResponseEntity<?> deleteOrganizer(@PathVariable Integer id) {
        organizerService.deleteOrganizer(id);
        return ResponseEntity.status(200).body(new ApiResponse("Organizer deleted successfully"));
    }
}
