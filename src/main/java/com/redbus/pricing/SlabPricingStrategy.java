package com.redbus.pricing;

import com.redbus.model.City;
import com.redbus.model.Trip;
import com.redbus.util.DistanceUtil;
import org.springframework.stereotype.Component;

@Component
public class SlabPricingStrategy implements PricingStrategy {
    
    @Override
    public long quote(Trip trip, City source, City destination, int seats, double occupancyRatio) {
        double distance = DistanceUtil.calculateDistance(source, destination);
        
        // ₹50 for first 10km, then ₹25 per 10km (round up per 10km slab)
        long basePrice;
        if (distance <= 10) {
            basePrice = 5000; // ₹50 in paise
        } else {
            double remainingDistance = distance - 10;
            int slabs = (int) Math.ceil(remainingDistance / 10.0);
            basePrice = 5000 + (slabs * 2500); // ₹50 + slabs * ₹25 in paise
        }
        
        return basePrice * seats;
    }
}
