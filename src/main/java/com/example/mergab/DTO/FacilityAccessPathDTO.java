package com.example.mergab.DTO;



import jakarta.validation.constraints.*;
import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class FacilityAccessPathDTO {

    @NotBlank(message = "Path points are required")
    private String pathPoints;

    @NotBlank(message = "Direction is required")
    @Pattern(regexp = "TO_FACILITY|FROM_FACILITY|BOTH",
            message = "Invalid access direction")
    private String direction = "BOTH";

    @NotNull(message = "Active status is required")
    private Boolean active = true;
}
