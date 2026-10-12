package com.example.mergab.Controller;

import com.example.mergab.Api.ApiResponse;
import com.example.mergab.Model.HistoricalAttendance;
import com.example.mergab.Service.HistoricalAttendanceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/attendance")
@RequiredArgsConstructor
public class HistoricalAttendanceController {

    private final HistoricalAttendanceService historicalAttendanceService;

    @GetMapping("/get-all")
    public ResponseEntity<?> getAllAttendance() {
        return ResponseEntity.status(200).body(historicalAttendanceService.getAllAttendance());
    }

    @PostMapping("/add")
    public ResponseEntity<?> addAttendance(@RequestBody @Valid HistoricalAttendance attendance) {
        historicalAttendanceService.addAttendance(attendance);
        return ResponseEntity.status(200).body(new ApiResponse("Historical attendance added successfully"));
    }

    @PutMapping("/update/{id}")
    public ResponseEntity<?> updateAttendance(@PathVariable Integer id, @RequestBody @Valid HistoricalAttendance attendance) {
        historicalAttendanceService.updateAttendance(id, attendance);
        return ResponseEntity.status(200).body(new ApiResponse("Historical attendance updated successfully"));
    }

    @DeleteMapping("/delete/{id}")
    public ResponseEntity<?> deleteAttendance(@PathVariable Integer id) {
        historicalAttendanceService.deleteAttendance(id);
        return ResponseEntity.status(200).body(new ApiResponse("Historical attendance deleted successfully"));
    }

}
