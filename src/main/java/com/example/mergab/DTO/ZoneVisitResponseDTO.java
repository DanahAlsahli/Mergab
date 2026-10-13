package com.example.mergab.DTO;



import lombok.*;
import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ZoneVisitResponseDTO {

    private Integer visitId;
    private Integer visitorId;
    private Integer zoneId;
    private String zoneName;
    private LocalDateTime checkInTime;
    private LocalDateTime checkOutTime;
    private Boolean manualCheckOut;
}
