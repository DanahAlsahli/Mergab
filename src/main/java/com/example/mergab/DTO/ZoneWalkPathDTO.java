package com.example.mergab.DTO;



import jakarta.validation.constraints.*;
import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ZoneWalkPathDTO {

    @NotBlank(message = "Path name is required")
    private String name;

    @NotBlank(message = "Path points are required")
    private String pathPoints;

    @NotBlank(message = "Direction is required")
    @Pattern(regexp = "BOTH|FORWARD|REVERSE",
            message = "Invalid path direction")
    private String direction = "BOTH";

    @NotNull(message = "Active status is required")
    private Boolean active = true;
}
