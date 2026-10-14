package com.example.mergab.DTO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

// المصلى (Facility نوعه PRAYER_ROOM) زي ما يوصل للفرونت في تنبيه الأذان: مكانه + منطقته + زحمة منطقته
@Data
@AllArgsConstructor
@NoArgsConstructor
public class PrayerRoomDTO {

    private Integer id;             // Facility id
    private String name;
    private String gender;          // MALE, FEMALE أو BOTH
    private Integer zoneId;
    private String zoneName;
    private Double mapX;
    private Double mapY;
    private Integer occupancyPercent;
    private String crowdLevel;      // LOW, MEDIUM, HIGH, CRITICAL
}
