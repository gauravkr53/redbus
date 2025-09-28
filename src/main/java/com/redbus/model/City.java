package com.redbus.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class City {
    private String cityId;
    private String name;
    private double latitude;
    private double longitude;
}
