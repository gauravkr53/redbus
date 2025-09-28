package com.redbus.pricing;

import com.redbus.model.City;
import com.redbus.model.Trip;

public interface PricingStrategy {
    long quote(Trip trip, City source, City destination, int seats, double occupancyRatio);
}
