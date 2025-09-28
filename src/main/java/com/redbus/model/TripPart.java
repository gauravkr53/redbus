package com.redbus.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TripPart {
    private String tripPartId;
    private String tripId;
    private String date;
    private String sourceCityId;
    private String destCityId;
    private String sourceTime;
    private String destTime;
    private int sequence;
    private int capacity;
    private int availableSeats;
}
