package com.example.mergab.DTO;



import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class QrScanDTO {

    @NotBlank(message = "QR token is required")
    private String qrToken;
}
