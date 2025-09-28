package com.redbus.repository.impl;

import com.redbus.model.Trip;
import com.redbus.model.TripPart;
import com.redbus.repository.InventoryRepository;
import org.springframework.stereotype.Repository;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Repository
public class InMemoryInventoryRepository implements InventoryRepository {

    //Store trips by tripId
    private final Map<String, Trip> trips = new ConcurrentHashMap<>();

    //Store trip parts by tripId
    private final Map<String, List<TripPart>> tripParts = new ConcurrentHashMap<>();

    // Simulated Redis index for trip searches on source, destination, and date
    private final Map<String, List<String>> searchIndex = new ConcurrentHashMap<>();

    private final Map<String, List<TripPart>> tripPartsFromSourceOnDate = new ConcurrentHashMap<>();
    private final Map<String, List<TripPart>> tripPartsFromDestinationOnDate = new ConcurrentHashMap<>();


    @Override
    public List<String> getTripIdsFromRedis(String sourceCityId, String destCityId, String date) {
        String key = getRedisCacheKey(sourceCityId, destCityId, date);
        return searchIndex.getOrDefault(key, Collections.emptyList());
    }

    @Override
    public void setTripIdsInRedis(String sourceCityId, String destCityId, String date, List<String> tripIds) {
        String key = getRedisCacheKey(sourceCityId, destCityId, date);
        searchIndex.put(key, new ArrayList<>(tripIds));
    }

    @Override
    public void invalidateTripIdsInRedis(String sourceCityId, String destCityId, String date) {
        String key = getRedisCacheKey(sourceCityId, destCityId, date);
        searchIndex.remove(key);
    }

    @Override
    public List<Trip> findTripsByIds(List<String> ids) {
        return ids.stream()
                .map(trips::get)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    @Override
    public Optional<Trip> findTrip(String tripId) {
        return Optional.ofNullable(trips.get(tripId));
    }

    @Override
    public void upsertTrip(Trip trip) {
        trips.put(trip.getTripId(), trip);
        
        // Update the "all_trips" index
        List<String> allTripIds = searchIndex.getOrDefault("all_trips", new ArrayList<>());
        if (!allTripIds.contains(trip.getTripId())) {
            allTripIds.add(trip.getTripId());
            searchIndex.put("all_trips", allTripIds);
        }
    }

    @Override
    public List<TripPart> findPartsByTrip(String tripId) {
        return tripParts.getOrDefault(tripId, Collections.emptyList()).stream()
                .sorted(Comparator.comparingInt(TripPart::getSequence))
                .collect(Collectors.toList());
    }

    @Override
    public void upsertParts(List<TripPart> parts) {
        if (!parts.isEmpty()) {
            String tripId = parts.get(0).getTripId();
            tripParts.put(tripId, new ArrayList<>(parts));
            
            // Initialize available seats for each trip part
            for (TripPart part : parts) {
                tripPartsFromSourceOnDate.putIfAbsent(getTripPartKey(part.getSourceCityId(), part.getDate()), new ArrayList<>());
                tripPartsFromSourceOnDate.get(getTripPartKey(part.getSourceCityId(), part.getDate())).add(part);
                tripPartsFromDestinationOnDate.putIfAbsent(getTripPartKey(part.getDestCityId(), part.getDate()), new ArrayList<>());
                tripPartsFromDestinationOnDate.get(getTripPartKey(part.getDestCityId(), part.getDate())).add(part);
            }

            for(int i = 0; i < parts.size(); i++) {
                System.out.println("Trip Part: " + parts.get(i));
                for(int j = 0; j < parts.size(); j++) {
                    invalidateTripIdsInRedis(parts.get(i).getSourceCityId(), parts.get(j).getDestCityId(), parts.get(i).getDate());
                }
            }
        }
    }

    @Override
    public boolean decrementSeats(String tripPartId, int seats) {
        try {
            TripPart part = findTripPartByScheduleId(tripPartId);
            if (part == null || part.getAvailableSeats() < seats) {
                return false; // Not enough seats
            }
            part.setAvailableSeats(part.getAvailableSeats() - seats);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public void incrementSeats(String tripPartId, int seats) {
        TripPart part = findTripPartByScheduleId(tripPartId);
        part.setAvailableSeats(part.getAvailableSeats() + seats);
    }

    @Override
    public List<String> searchTrips(String sourceCityId, String destCityId, String date) {
        tripParts.values().stream().forEach((value) -> System.out.println("tripsParts: " + value));; 
        List<TripPart> sourceParts = getTripPartsFromSource(sourceCityId, date);
        System.out.println("Source Parts: " + sourceParts);
        List<TripPart> destParts = getTripPartsFromDestination(destCityId, date);
        System.out.println("Dest Parts: " + destParts);
        // Find trips that have both source and destination parts
        Set<String> sourceTripIds = sourceParts.stream()
                .map(TripPart::getTripId)
                .collect(Collectors.toSet());
        
        Set<String> destTripIds = destParts.stream()
                .map(TripPart::getTripId)
                .collect(Collectors.toSet());
        
        // Find common trip IDs
        Set<String> commonTripIds = new HashSet<>(sourceTripIds);
        commonTripIds.retainAll(destTripIds);
        
        // Filter by sequence order (source should come before destination)
        return commonTripIds.stream()
                .filter(tripId -> {
                    List<TripPart> allParts = tripParts.getOrDefault(tripId, Collections.emptyList());
                    TripPart sourcePart = allParts.stream()
                            .filter(part -> part.getSourceCityId().equals(sourceCityId))
                            .findFirst().orElse(null);
                    TripPart destPart = allParts.stream()
                            .filter(part -> part.getDestCityId().equals(destCityId))
                            .findFirst().orElse(null);
                    
                    return sourcePart != null && destPart != null && 
                           sourcePart.getSequence() <= destPart.getSequence();
                })
                .collect(Collectors.toList());
    }

    @Override
    public List<Trip> findAllTrips() {
        return new ArrayList<>(trips.values());
    }

    private TripPart findTripPartByScheduleId(String tripPartId) {
        return tripParts.values().stream()
                .flatMap(List::stream)
                .filter(part -> part.getTripPartId().equals(tripPartId))
                .findFirst()
                .orElse(null);
    }

    private List<TripPart> getTripPartsFromSource(String sourceCityId, String date) {
        return tripPartsFromSourceOnDate.getOrDefault(getTripPartKey(sourceCityId, date), Collections.emptyList());
    }

    private List<TripPart> getTripPartsFromDestination(String destCityId, String date) {
        return tripPartsFromDestinationOnDate.getOrDefault(getTripPartKey(destCityId, date), Collections.emptyList());
    }

    private String getRedisCacheKey(String sourceCityId, String destCityId, String date) {
        return sourceCityId + "_" + destCityId + "_" + date;
    }

    private String getTripPartKey(String cityId, String date) {
        return cityId + "_" + date;
    }
}
