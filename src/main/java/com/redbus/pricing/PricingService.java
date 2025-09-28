package com.redbus.pricing;

import com.redbus.model.City;
import com.redbus.model.Trip;
import com.redbus.model.TripPart;
import com.redbus.repository.InventoryRepository;
import java.util.List;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PricingService {

    private final PricingStrategyFactory pricingStrategyFactory;

    public long calculatePrice(Trip trip, City source, City destination, int seats, int availableSeats, int totalCapacity) {
        // Calculate occupancy ratio based on the first trip part (assuming similar occupancy across parts)
        
        double occupancyRatio = (totalCapacity - availableSeats) / (double) totalCapacity;

        return pricingStrategyFactory.getPricingStrategy(trip.getPricingType())
                .quote(trip, source, destination, seats, occupancyRatio);
    }
}
