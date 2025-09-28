package com.redbus.service;

import com.redbus.dto.CreateTripRequest;
import com.redbus.dto.TripView;
import com.redbus.model.Trip;
import com.redbus.model.TripPart;
import com.redbus.model.PricingType;
import com.redbus.repository.CityRepository;
import com.redbus.repository.InventoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminService {
    
    private final InventoryRepository inventoryRepository;
    private final CityRepository cityRepository;
    
    public List<Trip> createTrip(CreateTripRequest request) {
        List<Trip> createdTrips = new ArrayList<>();
        
        // Determine the dates to create trips for
        List<LocalDate> dates = getDatesForRepeat(request.getDate(), request.getRepeatOption());
        
        for (LocalDate date : dates) {
            Trip trip = createSingleTrip(request, date);
            createdTrips.add(trip);
        }
        
        return createdTrips;
    }
    
    private List<LocalDate> getDatesForRepeat(LocalDate startDate, String repeatOption) {
        List<LocalDate> dates = new ArrayList<>();
        
        switch (repeatOption.toLowerCase()) {
            case "single":
                dates.add(startDate);
                break;
            case "7days":
                for (int i = 0; i < 7; i++) {
                    dates.add(startDate.plusDays(i));
                }
                break;
            case "30days":
                for (int i = 0; i < 30; i++) {
                    dates.add(startDate.plusDays(i));
                }
                break;
            default:
                throw new IllegalArgumentException("Invalid repeat option: " + repeatOption);
        }
        
        return dates;
    }
    
    private Trip createSingleTrip(CreateTripRequest request, LocalDate date) {
        // Create the main trip
        Trip trip = Trip.builder()
                .tripId(UUID.randomUUID().toString())
                .busId(request.getBusId())
                .date(date.toString())
                .sourceCityId(request.getSourceCityId())
                .destCityId(request.getDestCityId())
                .capacity(request.getCapacity())
                .pricingType(PricingType.valueOf(request.getPricingType() != null ? request.getPricingType() : "SLAB_50_FIRST_10KM_THEN_25_PER_10KM"))
                .build();
        
        // Create trip parts for the route
        List<TripPart> tripParts = createTripParts(trip, request);
        
        // Save trip and trip parts to repository
        inventoryRepository.upsertTrip(trip);
        inventoryRepository.upsertParts(tripParts);
        
        log.info("Created trip {} with {} parts for date {}", trip.getTripId(), tripParts.size(), date);
        
        return trip;
    }
    
    private List<TripPart> createTripParts(Trip trip, CreateTripRequest request) {
        List<TripPart> tripParts = new ArrayList<>();
        
        // Build the complete route: source -> stops -> destination
        List<String> route = new ArrayList<>();
        route.add(request.getSourceCityId());
        route.addAll(request.getStops());
        route.add(request.getDestCityId());
        
        // Calculate time intervals
        LocalTime departureTime = request.getDepartureTime();
        LocalTime arrivalTime = request.getArrivalTime();
        
        // Calculate total journey duration in minutes
        long totalMinutes = java.time.Duration.between(departureTime, arrivalTime).toMinutes();
        
        // Distribute time across segments (each segment gets equal time)
        int segmentCount = route.size() - 1;
        long minutesPerSegment = totalMinutes / segmentCount;
        
        // Create trip parts
        for (int i = 0; i < segmentCount; i++) {
            String sourceCityId = route.get(i);
            String destCityId = route.get(i + 1);
            
            LocalTime segmentDepartureTime = departureTime.plusMinutes((long) i * minutesPerSegment);
            LocalTime segmentArrivalTime = departureTime.plusMinutes((long) (i + 1) * minutesPerSegment);
            
            TripPart tripPart = TripPart.builder()
                    .tripPartId(UUID.randomUUID().toString())
                    .tripId(trip.getTripId())
                    .date(trip.getDate())
                    .sourceCityId(sourceCityId)
                    .destCityId(destCityId)
                    .sourceTime(segmentDepartureTime.toString())
                    .destTime(segmentArrivalTime.toString())
                    .sequence(i + 1)
                    .capacity(trip.getCapacity())
                    .availableSeats(trip.getCapacity())
                    .build();
            
            tripParts.add(tripPart);
        }
        
        return tripParts;
    }
    
    public List<TripView> getAllTripsForDisplay() {
        try {
            List<Trip> trips = inventoryRepository.findAllTrips();
            log.info("Found {} trips in repository", trips.size());
            
            return trips.stream()
                    .map(this::enrichTripForDisplay)
                    .toList();
                    
        } catch (Exception e) {
            log.error("Error loading trips for admin view: {}", e.getMessage(), e);
            return List.of();
        }
    }
    
    public TripView enrichTripForDisplay(Trip trip) {
        try {
            // Get city names for source and destination
            String sourceCityName = cityRepository.findById(trip.getSourceCityId())
                    .map(city -> city.getName())
                    .orElse(trip.getSourceCityId());
            
            String destCityName = cityRepository.findById(trip.getDestCityId())
                    .map(city -> city.getName())
                    .orElse(trip.getDestCityId());
            
            // Get trip parts to extract timing and stops information
            List<TripPart> tripParts = inventoryRepository.findPartsByTrip(trip.getTripId());
            System.out.println("Trip Parts: tripId: " + trip.getTripId() + " tripParts: " + tripParts);
            
            String departureTime = tripParts.isEmpty() ? "N/A" : tripParts.get(0).getSourceTime();
            String arrivalTime = tripParts.isEmpty() ? "N/A" : tripParts.get(tripParts.size() - 1).getDestTime();
            
            // Extract intermediate stops (excluding source and destination)
            List<String> stops = tripParts.stream()
                    .skip(1) // Skip the first part (source to first stop)
                    .limit(tripParts.size() - 1) // Exclude the last part (last stop to destination)
                    .map(TripPart::getSourceCityId)
                    .map(cityId -> cityRepository.findById(cityId).map(city -> city.getName()).orElse(cityId))
                    .toList();
            
            System.out.println("Stops: " + stops);
            
            return TripView.builder()
                    .tripId(trip.getTripId())
                    .busId(trip.getBusId())
                    .date(trip.getDate())
                    .source(sourceCityName)
                    .dest(destCityName)
                    .capacity(trip.getCapacity())
                    .pricingType(trip.getPricingType().name())
                    .departureTime(departureTime)
                    .arrivalTime(arrivalTime)
                    .stops(stops)
                    .build();
                    
        } catch (Exception e) {
            log.error("Error enriching trip {} for display: {}", trip.getTripId(), e.getMessage());
            // Return basic trip view if enrichment fails
            return TripView.builder()
                    .tripId(trip.getTripId())
                    .busId(trip.getBusId())
                    .date(trip.getDate())
                    .source(trip.getSourceCityId())
                    .dest(trip.getDestCityId())
                    .capacity(trip.getCapacity())
                    .pricingType(trip.getPricingType().name())
                    .departureTime("N/A")
                    .arrivalTime("N/A")
                    .stops(List.of())
                    .build();
        }
    }
}
