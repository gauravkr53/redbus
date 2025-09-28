package com.redbus.dto;

import com.redbus.model.BookingStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookingView {
    private String bookingId;
    private BookingStatus status;
    private String tripId;
    private String date;
    private String source;
    private String dest;
    private String sourceCityId;
    private String destCityId;
    private String sourceTime;
    private String destTime;
    private int seats;
    private LocalDateTime createdAt;
    private long pricePaise;
}
