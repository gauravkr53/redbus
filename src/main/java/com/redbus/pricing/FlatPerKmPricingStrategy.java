package com.redbus.pricing;

import com.redbus.model.City;
import com.redbus.model.Trip;
import com.redbus.util.DistanceUtil;
import org.springframework.stereotype.Component;

@Component
public class FlatPerKmPricingStrategy implements PricingStrategy {
    
    @Override
    public long quote(Trip trip, City source, City destination, int seats, double occupancyRatio) {
        double distance = DistanceUtil.calculateDistance(source, destination);
        
        // ₹3/km × seats
        long basePrice = (long) (distance * 300); // ₹3 in paise
        return basePrice * seats;
    }
}
