package com.redbus.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Trip {
    private String tripId;
    private String busId;
    private String date;
    private String sourceCityId;
    private String destCityId;
    private int capacity;
    private PricingType pricingType;
}
