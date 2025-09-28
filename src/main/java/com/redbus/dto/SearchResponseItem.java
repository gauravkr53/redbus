package com.redbus.dto;

import com.redbus.model.Trip;
import com.redbus.model.TripPart;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SearchResponseItem {
    private String tripId;
    private String busId;
    private String date;
    private String source;
    private String dest;
    private String sourceCityId;
    private String destCityId;
    private String sourceTime;
    private String destTime;
    private int capacity;
    private int availableSeats;
    private BusInfo bus;
    private long pricePaise;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BusInfo {
        private String operator;
    }
}
