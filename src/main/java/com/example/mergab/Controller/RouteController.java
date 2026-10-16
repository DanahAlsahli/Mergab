
package com.example.mergab.Controller;

import com.example.mergab.DTO.RouteRequestDTO;
import com.example.mergab.DTO.RouteResponseDTO;
import com.example.mergab.Service.RouteRecommendationService;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

import java.util.Objects;

@RestController
@RequestMapping("/api/v1/routes")
public class RouteController {

    private final RouteRecommendationService routeRecommendationService;

    public RouteController(
            RouteRecommendationService routeRecommendationService) {
        this.routeRecommendationService = routeRecommendationService;
    }

    @PostMapping("/recommend")
    public ResponseEntity<RouteResponseDTO> recommendRoute(
            @Valid @RequestBody RouteRequestDTO request) {

        RouteResponseDTO response =
                routeRecommendationService.recommendRoute(
                        request.getCurrentZoneId(),
                        request.getDestinationZoneId(),
                        request.getSimulationId()
                );

        return ResponseEntity.ok(response);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(
            MethodArgumentNotValidException exception) {

        FieldError fieldError = exception.getBindingResult()
                .getFieldErrors()
                .stream()
                .findFirst()
                .orElse(null);

        String message = fieldError != null
                ? Objects.requireNonNullElse(
                fieldError.getDefaultMessage(),
                "Request validation failed")
                : "Request validation failed";

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse(message));
    }

    public static class ErrorResponse {

        private final String message;

        public ErrorResponse(String message) {
            this.message = message;
        }

        public String getMessage() {
            return message;
        }
    }
}
