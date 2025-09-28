package com.redbus.pricing;

import org.springframework.stereotype.Service;

@Service
public class PricingStrategyFactory {

    private final SlabPricingStrategy slabPricingStrategy;
    private final FlatPerKmPricingStrategy flatPerKmPricingStrategy;
    private final SurgePricingStrategy surgePricingStrategy;

    public PricingStrategyFactory(
                          SlabPricingStrategy slabPricingStrategy,
                          FlatPerKmPricingStrategy flatPerKmPricingStrategy,
                          SurgePricingStrategy surgePricingStrategy) {
        this.slabPricingStrategy = slabPricingStrategy;
        this.flatPerKmPricingStrategy = flatPerKmPricingStrategy;
        this.surgePricingStrategy = surgePricingStrategy;
    }

    public PricingStrategy getPricingStrategy(com.redbus.model.PricingType pricingType) {
        return switch (pricingType) {
            case SLAB_50_FIRST_10KM_THEN_25_PER_10KM -> slabPricingStrategy;
            case FLAT_PER_KM -> flatPerKmPricingStrategy;
            case SURGE_BY_OCCUPANCY -> surgePricingStrategy;
        };
    }

}
