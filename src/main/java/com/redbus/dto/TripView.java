package com.redbus.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TripView {
    private String tripId;
    private String busId;
    private String date;
    private String source;
    private String dest;
    private int capacity;
    private String pricingType;
    private String departureTime;
    private String arrivalTime;
    private List<String> stops;
}
