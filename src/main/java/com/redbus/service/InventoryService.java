package com.redbus.service;

import com.redbus.dto.SearchResponseItem;
import com.redbus.model.Bus;
import com.redbus.model.City;
import com.redbus.model.Trip;
import com.redbus.model.TripPart;
import com.redbus.pricing.PricingService;
import com.redbus.repository.BusRepository;
import com.redbus.repository.CityRepository;
import com.redbus.repository.InventoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@RequiredArgsConstructor
public class InventoryService {
    private final InventoryRepository inventoryRepository;
    private final CityRepository cityRepository;
    private final BusRepository busRepository;
    private final PricingService pricingService;

    public List<Trip> searchTrips(String sourceCityId, String destCityId, String date) {
        // First check Redis cache
        List<String> tripIds = inventoryRepository.getTripIdsFromRedis(sourceCityId, destCityId, date);

        if (tripIds.isEmpty()) {
            // Index miss - compute from all trips and populate index
            // First, get all trips for the date
            tripIds = inventoryRepository.searchTrips(sourceCityId, destCityId, date);

            // Cache the result in Redis
            inventoryRepository.setTripIdsInRedis(sourceCityId, destCityId, date, tripIds);
        }

        return inventoryRepository.findTripsByIds(tripIds);
    }

    public List<TripPart> getTripParts(String tripId) {
        return inventoryRepository.findPartsByTrip(tripId);
    }

    public Optional<Trip> getTrip(String tripId) {
        return inventoryRepository.findTrip(tripId);
    }

    public int getAvailableSeats(String tripId, String sourceCityId, String destCityId) {
        List<TripPart> matchingParts = findSegmentsForRoute(tripId, sourceCityId, destCityId);
        System.out.println("Get Available Seats: Matching Parts: " + matchingParts);
        return getAvailableSeats(matchingParts);
    }

    public int getTotalCapacity(List<TripPart> parts) {
        return parts.stream()
                .map(TripPart::getCapacity)
                .reduce(0, Integer::sum);
    }

    public int getAvailableSeats(List<TripPart> parts) {
        return parts.stream()
                .map(TripPart::getAvailableSeats)
                .reduce((a, b) -> Math.min(a, b)).orElse(0);
    }

    public List<TripPart> findSegmentsForRoute(String tripId, String sourceCityId, String destCityId) {

        List<TripPart> allParts = getTripParts(tripId);

        // First check for direct segment
        TripPart sourcePart = allParts.stream()
                .filter(part -> part.getSourceCityId().equals(sourceCityId))
                .findFirst()
                .orElse(null);
        TripPart destPart = allParts.stream()
                .filter(part -> part.getDestCityId().equals(destCityId))
                .findFirst()
                .orElse(null);
        if (sourcePart != null && destPart != null) {
            return allParts.stream()
                .filter(part -> part.getSequence() >= sourcePart.getSequence() && part.getSequence() <= destPart.getSequence())
                .sorted(Comparator.comparingInt(TripPart::getSequence))
                .toList();
        }
        return Collections.emptyList();
    }

    public List<SearchResponseItem> search(String sourceCityId, String destCityId, String date) {
        System.out.println("Search: Source City ID: " + sourceCityId);
        System.out.println("Search: Dest City ID: " + destCityId);
        System.out.println("Search: Date: " + date);
        System.out.println("Search: Inventory Repository: " + inventoryRepository.findAllTrips());
        List<Trip> trips = searchTrips(sourceCityId, destCityId, date);

        System.out.println("Search: Trips: " + trips);
        return trips.stream()
                .map(trip -> createSearchResponseItem(trip, sourceCityId, destCityId))
                .toList();
    }

    public SearchResponseItem getTripDetails(String tripId, String sourceCityId, String destCityId) {
        Optional<Trip> tripOpt = getTrip(tripId);
        if (tripOpt.isEmpty()) {
            return null;
        }
        Trip trip = tripOpt.get();
        return createSearchResponseItem(trip, sourceCityId, destCityId);
    }

    private SearchResponseItem createSearchResponseItem(Trip trip, String sourceCityId, String destCityId) {
        Optional<City> sourceCity = cityRepository.findById(trip.getSourceCityId());
        Optional<City> destCity = cityRepository.findById(trip.getDestCityId());
        Optional<Bus> bus = busRepository.findById(trip.getBusId());

        List<TripPart> tripParts = findSegmentsForRoute(trip.getTripId(), sourceCityId, destCityId);
        
        int availableSeats = getAvailableSeats(tripParts);
        int totalCapacity = getTotalCapacity(tripParts);
        System.out.println("Create Search Response Item: Trip Parts: " + tripParts);
        System.out.println("Create Search Response Item: Available Seats: " + availableSeats);
        System.out.println("Create Search Response Item: Total Capacity: " + totalCapacity);
        
        // Calculate price for 1 seat as estimate (using first matching trip part)
        long pricePaise = 0;
        if (sourceCity.isPresent() && destCity.isPresent() && !tripParts.isEmpty()) {
            pricePaise = pricingService.calculatePrice(trip, sourceCity.get(), destCity.get(), 1, availableSeats, totalCapacity);
        }
        
        // Get city names for display
        Optional<City> searchSourceCity = cityRepository.findById(sourceCityId);
        Optional<City> searchDestCity = cityRepository.findById(destCityId);
        
        // Get time information from the first trip part
        String sourceTime = !tripParts.isEmpty() ? tripParts.get(0).getSourceTime() : null;
        String destTime = !tripParts.isEmpty() ? tripParts.get(tripParts.size() - 1).getDestTime() : null;
        
        return SearchResponseItem.builder()
                .tripId(trip.getTripId())
                .busId(trip.getBusId())
                .date(trip.getDate())
                .source(searchSourceCity.map(City::getName).orElse(null))
                .dest(searchDestCity.map(City::getName).orElse(null))
                .sourceCityId(sourceCityId)
                .destCityId(destCityId)
                .sourceTime(sourceTime)
                .destTime(destTime)
                .capacity(trip.getCapacity())
                .availableSeats(availableSeats)
                .bus(SearchResponseItem.BusInfo.builder()
                        .operator(bus.map(Bus::getOperator).orElse("Unknown"))
                        .build())
                .pricePaise(pricePaise)
                .build();
    }
}
