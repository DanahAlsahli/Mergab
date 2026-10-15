package com.example.mergab.DTO;



import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class FacilityDTO {

    @NotBlank(message = "Facility name is required")
    private String name;

    @NotBlank(message = "Facility type is required")
    @Pattern(regexp = "RESTROOM|PRAYER_ROOM|EXIT|ENTRANCE",
            message = "Invalid facility type")
    private String type;

    @NotBlank(message = "Gender is required")
    @Pattern(regexp = "MALE|FEMALE|BOTH",
            message = "Invalid gender")
    private String gender;

    // Real GPS coordinates (optional)
    @DecimalMin(value = "-90.0", message = "Invalid latitude")
    @DecimalMax(value = "90.0", message = "Invalid latitude")
    private Double latitude;

    @DecimalMin(value = "-180.0", message = "Invalid longitude")
    @DecimalMax(value = "180.0", message = "Invalid longitude")
    private Double longitude;

    // Virtual Expo map coordinates (0–100%)
    @DecimalMin(value = "0.0", message = "mapX cannot be negative")
    @DecimalMax(value = "100.0", message = "mapX cannot exceed 100")
    private Double mapX;

    @DecimalMin(value = "0.0", message = "mapY cannot be negative")
    @DecimalMax(value = "100.0", message = "mapY cannot exceed 100")
    private Double mapY;

    // Require at least one complete coordinate pair
    @AssertTrue(message = "Provide complete GPS or map coordinates")
    public boolean isValidLocation() {

        boolean hasGPS = latitude != null && longitude != null;
        boolean hasMap = mapX != null && mapY != null;

        boolean partialGPS = (latitude == null) != (longitude == null);
        boolean partialMap = (mapX == null) != (mapY == null);

        return !partialGPS && !partialMap && (hasGPS || hasMap);
    }
}
