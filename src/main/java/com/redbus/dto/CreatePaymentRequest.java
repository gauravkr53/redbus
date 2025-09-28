package com.redbus.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreatePaymentRequest {
    @NotBlank(message = "Booking ID is required")
    private String bookingId;

    @NotBlank(message = "Payment method is required")
    private String method;
}
