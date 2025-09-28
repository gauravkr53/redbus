package com.redbus.repository;

import com.redbus.model.Trip;
import com.redbus.model.TripPart;
import java.util.List;
import java.util.Optional;

public interface InventoryRepository {

    // Redis operations:- These 2 operations will interact with Redis, while others will interact with DB
    List<String> getTripIdsFromRedis(String sourceCityId, String destCityId, String date);
    void setTripIdsInRedis(String sourceCityId, String destCityId, String date, List<String> tripIds);
    void invalidateTripIdsInRedis(String sourceCityId, String destCityId, String date)
    
    // Trip operations
    List<Trip> findTripsByIds(List<String> ids);
    Optional<Trip> findTrip(String tripId);
    void upsertTrip(Trip trip);
    
    // TripPart operations
    List<TripPart> findPartsByTrip(String tripId);
    void upsertParts(List<TripPart> parts);
    
    // Seat availability operations (per trip part)
    boolean decrementSeats(String tripPartId, int seats);
    void incrementSeats(String tripPartId, int seats);
    
    // Helper method to get all trip parts for a trip

    // Search trips by source and destination
    List<String> searchTrips(String sourceCityId, String destCityId, String date);

    List<Trip> findAllTrips();
}
