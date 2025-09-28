package com.redbus.pricing;

import com.redbus.model.City;
import com.redbus.model.Trip;
import com.redbus.util.DistanceUtil;
import org.springframework.stereotype.Component;

@Component
public class SurgePricingStrategy implements PricingStrategy {
    
    @Override
    public long quote(Trip trip, City source, City destination, int seats, double occupancyRatio) {
        double distance = DistanceUtil.calculateDistance(source, destination);
        
        // base ₹3/km × (1 + surge) × seats
        // surge = 0.5 if occupancy > 0.8 else 0
        double surge = occupancyRatio > 0.8 ? 0.5 : 0.0;
        double multiplier = 1.0 + surge;
        
        long basePrice = (long) (distance * 300 * multiplier); // ₹3 in paise
        return basePrice * seats;
    }
}
