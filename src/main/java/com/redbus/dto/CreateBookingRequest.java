package com.redbus.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateBookingRequest {
    @NotBlank(message = "Trip ID is required")
    private String tripId;

    @NotEmpty(message = "source city id is required")
    private String sourceCityId;

    @NotEmpty(message = "destination city id is required")
    private String destCityId;

    @NotNull(message = "Number of seats is required")
    @Min(value = 1, message = "At least 1 seat is required")
    private Integer seats;
}
