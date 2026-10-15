package com.example.mergab.DTO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

// رد GET /prayer/alert/{visitorId}
// show = true → الفرونت يطلع التنبيه بأقرب مصلى رجال وأقرب مصلى نساء
@Data
@AllArgsConstructor
@NoArgsConstructor
public class PrayerAlertDTO {

    private Boolean show = false;
    private String prayer;          // الصلاة الجاية: fajr, dhuhr, asr, maghrib, isha
    private String time;            // وقت الأذان HH:mm
    private Integer minutesLeft;    // كم باقي على الأذان
    private Integer fromZoneId;     // المنطقة اللي انحسب منها الأقرب (null = ما نعرف مكانه)
    private PrayerRoomDTO men;
    private PrayerRoomDTO women;
}
