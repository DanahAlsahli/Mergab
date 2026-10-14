package com.example.mergab.Controller;

import com.example.mergab.Api.ApiResponse;
import com.example.mergab.Model.Facility;
import com.example.mergab.Model.Zone;
import com.example.mergab.Repository.FacilityRepository;
import com.example.mergab.Service.PrayerAlertService;
import com.example.mergab.Service.PrayerTimesService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Comparator;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/prayer")
@RequiredArgsConstructor
public class PrayerController {

    private final PrayerTimesService prayerTimesService;
    private final FacilityRepository facilityRepository;
    private final PrayerAlertService prayerAlertService;

    @GetMapping("/today")
    public ResponseEntity<?> getToday(@RequestParam(required = false) String simulateTime) {
        Map<String, Object> res = prayerTimesService.getToday(simulateTime);
        List<Zone> prayerZones = facilityRepository
                .findFacilitiesByTypeAndGenderIn("PRAYER_ROOM", List.of("MALE", "FEMALE", "BOTH"))
                .stream()
                .map(Facility::getZone)
                .distinct()
                .sorted(Comparator.comparing(Zone::getId))
                .toList();
        res.put("prayerZones", prayerZones);
        return ResponseEntity.status(200).body(res);
    }

    // تنبيه قبل الأذان بـ 10 دقائق: أقرب مصلى رجال ونساء بأهدأ طريق
    // fromZoneId اختياري (لو الزائر ما سوّى check-in بالـ QR)، simulateTime اختياري للتجربة (HH:mm)
    @GetMapping("/alert/{visitorId}")
    public ResponseEntity<?> getAlert(@PathVariable Integer visitorId,
                                      @RequestParam(required = false) Integer fromZoneId,
                                      @RequestParam(required = false) String simulateTime) {
        return ResponseEntity.status(200).body(prayerAlertService.getAlert(visitorId, fromZoneId, simulateTime));
    }

    // الفرونت يناديها بعد ما الزائر يشوف التنبيه عشان ما يطلع له مرة ثانية لنفس الصلاة
    @PutMapping("/alert/{visitorId}/seen/{prayer}")
    public ResponseEntity<?> markAlertSeen(@PathVariable Integer visitorId, @PathVariable String prayer) {
        prayerAlertService.markSeen(visitorId, prayer);
        return ResponseEntity.status(200).body(new ApiResponse("Prayer alert marked as seen"));
    }
}
