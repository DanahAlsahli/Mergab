package com.example.mergab.Controller;

import com.example.mergab.Repository.ZoneRepository;
import com.example.mergab.Service.PrayerTimesService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/prayer")
@RequiredArgsConstructor
public class PrayerController {

    private final PrayerTimesService prayerTimesService;
    private final ZoneRepository zoneRepository;

    @GetMapping("/today")
    public ResponseEntity<?> getToday(@RequestParam(required = false) String simulateTime) {
        Map<String, Object> res = prayerTimesService.getToday(simulateTime);
        res.put("prayerZones", zoneRepository.findAllByHasPrayerAreaTrue());
        return ResponseEntity.status(200).body(res);
    }
}
