package com.example.mergab.Controller;

import com.example.mergab.Api.ApiResponse;
import com.example.mergab.DTO.VisitorDTO;
import com.example.mergab.Service.VisitorService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/visitor")
@RequiredArgsConstructor
public class VisitorController {

    private final VisitorService visitorService;

    @GetMapping("/get-all")
    public ResponseEntity<?> getAllVisitors() {
        return ResponseEntity.status(200).body(visitorService.getAllVisitors());
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody @Valid VisitorDTO dto) {
        visitorService.register(dto);
        return ResponseEntity.status(201).body(new ApiResponse("Visitor registered successfully"));
    }

    @PutMapping("/update/{id}")
    public ResponseEntity<?> updateVisitor(@PathVariable Integer id, @RequestBody @Valid VisitorDTO dto) {
        visitorService.updateVisitor(id, dto);
        return ResponseEntity.status(200).body(new ApiResponse("Visitor updated successfully"));
    }

    @DeleteMapping("/delete/{id}")
    public ResponseEntity<?> deleteVisitor(@PathVariable Integer id) {
        visitorService.deleteVisitor(id);
        return ResponseEntity.status(200).body(new ApiResponse("Visitor deleted successfully"));
    }



    // Get the visitor's QR code as a PNG image
    @GetMapping(value = "/qr-code/{visitorId}", produces = MediaType.IMAGE_PNG_VALUE)
    public ResponseEntity<byte[]> getVisitorQrCode(@PathVariable Integer visitorId) {

        return ResponseEntity.status(200).contentType(MediaType.IMAGE_PNG).body(visitorService.getVisitorQrCode(visitorId));
    }

}
