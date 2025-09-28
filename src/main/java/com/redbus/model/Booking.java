package com.redbus.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Booking {
    private String bookingId;
    private String userId;
    private String tripId;
    private String sourceCityId;
    private String destCityId;
    private int seats;
    private BookingStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime expiresAt;
    private long pricePaise;
    private String paymentId;
}
