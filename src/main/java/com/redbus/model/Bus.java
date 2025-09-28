package com.redbus.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Bus {
    private String busId;
    private String ownerId;
    private String operator;
    private String parkingAddress;
}
