package com.redbus.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.*;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateTripRequest {
    
    @NotBlank(message = "Source city is required")
    private String sourceCityId;
    
    @NotBlank(message = "Destination city is required")
    private String destCityId;
    
    @NotEmpty(message = "At least one stop is required")
    private List<String> stops; // Intermediate stops (city IDs)
    
    @NotNull(message = "Date is required")
    private LocalDate date;
    
    @NotNull(message = "Departure time is required")
    private LocalTime departureTime;
    
    @NotNull(message = "Arrival time is required")
    private LocalTime arrivalTime;
    
    @NotBlank(message = "Bus ID is required")
    private String busId;
    
    @Min(value = 1, message = "Capacity must be at least 1")
    @Max(value = 100, message = "Capacity cannot exceed 100")
    private int capacity;
    
    @NotBlank(message = "Repeat option is required")
    private String repeatOption; // "single", "7days", "30days"
    
    private String pricingType; // Optional, defaults to "SLAB"
}
