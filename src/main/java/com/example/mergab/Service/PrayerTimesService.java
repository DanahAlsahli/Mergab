package com.example.mergab.Service;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class PrayerTimesService {

    private static final String URL =
            "https://api.aladhan.com/v1/timingsByCity?city=Riyadh&country=Saudi%20Arabia&method=4";
    private static final ZoneId RIYADH = ZoneId.of("Asia/Riyadh");
    private static final String[] KEYS = {"fajr", "dhuhr", "asr", "maghrib", "isha"};

    private final RestTemplate rest = new RestTemplate();
    private LocalDate cachedDay;
    private Map<String, String> cached;
    private String source;

    // يجيب الأوقات مرة باليوم ويخزنها
    public synchronized Map<String, String> getTodayTimes() {
        LocalDate today = LocalDate.now(RIYADH);
        if (cached != null && today.equals(cachedDay)) return cached;
        try {
            Map<?, ?> res = rest.getForObject(URL, Map.class);
            Map<?, ?> t = (Map<?, ?>) ((Map<?, ?>) res.get("data")).get("timings");
            Map<String, String> m = new LinkedHashMap<>();
            m.put("fajr",    t.get("Fajr").toString().substring(0, 5));
            m.put("dhuhr",   t.get("Dhuhr").toString().substring(0, 5));
            m.put("asr",     t.get("Asr").toString().substring(0, 5));
            m.put("maghrib", t.get("Maghrib").toString().substring(0, 5));
            m.put("isha",    t.get("Isha").toString().substring(0, 5));
            cached = m; cachedDay = today; source = "live";
        } catch (Exception e) {
            // احتياطي لو Aladhan ما رد (وما نخزنه عشان نحاول مرة ثانية)
            source = "sample";
            return Map.of("fajr", "04:41", "dhuhr", "11:44", "asr", "15:05", "maghrib", "17:35", "isha", "19:05");
        }
        return cached;
    }

    public String getSource() { return source; }

    public static int toMin(String hhmm) {
        String[] p = hhmm.split(":");
        return Integer.parseInt(p[0]) * 60 + Integer.parseInt(p[1]);
    }

    public Map<String, Object> getToday(String simulateTime) {
        Map<String, String> times = getTodayTimes();
        boolean simulated = simulateTime != null;
        int now = simulated ? toMin(simulateTime)
                : LocalTime.now(RIYADH).getHour() * 60 + LocalTime.now(RIYADH).getMinute();

        Map<String, Object> next = null;
        String current = null;
        for (String k : KEYS) {
            int diff = toMin(times.get(k)) - now;
            if (diff <= 0 && diff > -20) current = k;
            if (diff > 0 && next == null) next = Map.of("key", k, "time", times.get(k), "minutesLeft", diff);
        }
        if (next == null) // بعد العشاء → فجر بكرة
            next = Map.of("key", "fajr", "time", times.get("fajr"),
                    "minutesLeft", toMin(times.get("fajr")) + 1440 - now);

        Map<String, Object> res = new LinkedHashMap<>();
        res.put("prayers", times);
        res.put("nextPrayer", next);
        res.put("currentPrayer", current);
        res.put("localTime", String.format("%02d:%02d", now / 60, now % 60));
        res.put("source", source);
        res.put("simulated", simulated);
        return res;
    }
}
