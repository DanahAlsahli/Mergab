package com.example.mergab.Controller;



import com.example.mergab.DTO.EarlyWarningResponseDTO;
import com.example.mergab.Service.EarlyWarningService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/ai")
@RequiredArgsConstructor
public class EarlyWarningController {

    private final EarlyWarningService earlyWarningService;

    @GetMapping("/early-warnings/{simulationId}")
    public ResponseEntity<EarlyWarningResponseDTO> getEarlyWarnings(
            @PathVariable Integer simulationId) {

        return ResponseEntity.ok(
                earlyWarningService.getEarlyWarnings(simulationId)
        );
    }
}
