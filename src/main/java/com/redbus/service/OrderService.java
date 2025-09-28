package com.redbus.service;

import com.redbus.model.Booking;
import com.redbus.model.BookingStatus;
import com.redbus.model.TripPart;
import com.redbus.model.City;
import com.redbus.model.Trip;
import com.redbus.dto.BookingView;
import com.redbus.repository.BookingRepository;
import com.redbus.repository.InventoryRepository;
import com.redbus.repository.LockRepository;
import com.redbus.repository.CityRepository;
import com.redbus.pricing.PricingService;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.ArrayList;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {
    private final BookingRepository bookingRepository;
    private final InventoryRepository inventoryRepository;
    private final LockRepository lockRepository;
    private final InventoryService inventoryService;
    private final PricingService pricingService;
    private final CityRepository cityRepository;

    public Booking createBooking(String userId, String tripId, String sourceCityId, String destCityId, int seats) {
        String lockKey = "trip:" + tripId;
        
        if (!lockRepository.tryLock(lockKey)) {
            throw new IllegalStateException("Trip is currently being booked by another user");
        }
        try {
            List<TripPart> tripParts = inventoryService.findSegmentsForRoute(tripId, sourceCityId, destCityId);

            int availableSeats = inventoryService.getAvailableSeats(tripParts);
            if (availableSeats < seats) {
                lockRepository.unlock(lockKey);
                throw new IllegalArgumentException("Insufficient seats available for trip");
            }
    
            // Calculate price for the booking
            long pricePaise = computePrice(tripId, sourceCityId, destCityId, seats, availableSeats);
    
            // Create booking as RESERVED
            Booking booking = Booking.builder()
                    .bookingId(UUID.randomUUID().toString())
                    .userId(userId)
                    .tripId(tripId)
                    .sourceCityId(sourceCityId)
                    .destCityId(destCityId)
                    .seats(seats)
                    .status(BookingStatus.RESERVED)
                    .createdAt(LocalDateTime.now())
                    .expiresAt(LocalDateTime.now().plusMinutes(5)) // 5-minute TTL
                    .pricePaise(pricePaise)
                    .build();

            // Atomically reserve seats for all trip parts
            for (TripPart tripPart : tripParts) {
                if (!inventoryRepository.decrementSeats(tripPart.getTripPartId(), seats)) {
                    throw new IllegalStateException("Failed to reserve seats for trip part: " + tripPart.getTripPartId());
                }
            }
            bookingRepository.save(booking);

            log.info("Created booking {} for trip {} with {} seats", booking.getBookingId(), tripId, seats);
            return booking;

        } finally {
            lockRepository.unlock(lockKey);
        }
    }

    public void confirmBooking(String bookingId, String paymentId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new IllegalArgumentException("Booking not found"));

        if (booking.getStatus() != BookingStatus.RESERVED) {
            throw new IllegalStateException("Booking is not in RESERVED status");
        }
        booking.setPaymentId(paymentId);
        booking.setStatus(BookingStatus.CONFIRMED);
        bookingRepository.save(booking);
        log.info("Confirmed booking {}", bookingId);
    }

    public void releaseBooking(String bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new IllegalArgumentException("Booking not found"));

        if (booking.getStatus() == BookingStatus.RESERVED) {
            // Release seats for all trip parts
            List<TripPart> tripParts = inventoryService.findSegmentsForRoute(booking.getTripId(), booking.getSourceCityId(), booking.getDestCityId());    
    
            for (TripPart tripPart : tripParts) {
                inventoryRepository.incrementSeats(tripPart.getTripPartId(), booking.getSeats());
            }
            bookingRepository.updateStatus(booking.getBookingId(), BookingStatus.EXPIRED);
            log.info("Realease booking {} and restored {} seats across {} trip parts", 
                    booking.getBookingId(), booking.getSeats(), tripParts.size());
        }
    }

    public void expireReservedBookings() {
        List<Booking> expiredBookings = bookingRepository.findExpiredReservations();
        
        for (Booking booking : expiredBookings) {
            releaseBooking(booking.getBookingId());
        }
        
        if (!expiredBookings.isEmpty()) {
            bookingRepository.deleteExpiredReservations();
        }
    }

    public List<BookingView> getAllBookings(String userId) {
        // Get CONFIRMED bookings
        List<Booking> allBookings = bookingRepository.findByUserIdAndStatus(userId, BookingStatus.CONFIRMED);
        
        List<BookingView> bookingViews = new ArrayList<>();
        
        for (Booking booking : allBookings) {
            try {
                BookingView bookingView = buildBookingView(booking);
                if (bookingView != null)  {
                    bookingViews.add(bookingView);
                }
            } catch (Exception e) {
                log.error("Error processing booking {}: {}", booking.getBookingId(), e.getMessage());
            }
        }
        
        return bookingViews;
    }

    private long computePrice(String tripId, String sourceCityId, String destCityId, int seats, int availableSeats) {
        Optional<Trip> trip = inventoryService.getTrip(tripId);
        if (trip.isEmpty()) {
            throw new IllegalArgumentException("Trip not found");
        }

        City sourceCity = cityRepository.findById(sourceCityId)
                .orElseThrow(() -> new IllegalArgumentException("Source city not found"));
        City destCity = cityRepository.findById(destCityId)
                .orElseThrow(() -> new IllegalArgumentException("Destination city not found"));

        return pricingService.calculatePrice(trip.get(), sourceCity, destCity, seats, availableSeats, trip.get().getCapacity());
    }

    private BookingView buildBookingView(Booking booking) {

        // Get trip details
        Optional<Trip> tripOpt = inventoryService.getTrip(booking.getTripId());
        if (tripOpt.isEmpty()) {
            log.warn("Trip not found for booking: {}", booking.getBookingId());
            return null;
        }

        Trip trip = tripOpt.get();

        // Get city details
        Optional<City> sourceCityOpt = cityRepository.findById(booking.getSourceCityId());
        Optional<City> destCityOpt = cityRepository.findById(booking.getDestCityId());

        if (sourceCityOpt.isEmpty() || destCityOpt.isEmpty()) {
            log.warn("City not found for booking: {}", booking.getBookingId());
            return null;
        }

        City sourceCity = sourceCityOpt.get();
        City destCity = destCityOpt.get();

        // Get trip parts for source and destination times
        List<TripPart> tripParts = inventoryService.findSegmentsForRoute(booking.getTripId(), booking.getSourceCityId(), booking.getDestCityId());

        String sourceTime = "";
        String destTime = "";
        if (!tripParts.isEmpty()) {
            sourceTime = tripParts.get(0).getSourceTime();
            destTime = tripParts.get(tripParts.size() - 1).getDestTime();
        }

        return BookingView.builder()
                .bookingId(booking.getBookingId())
                .status(booking.getStatus())
                .tripId(booking.getTripId())
                .date(trip.getDate())
                .source(sourceCity.getName())
                .dest(destCity.getName())
                .sourceCityId(booking.getSourceCityId())
                .destCityId(booking.getDestCityId())
                .sourceTime(sourceTime)
                .destTime(destTime)
                .seats(booking.getSeats())
                .createdAt(booking.getCreatedAt())
                .pricePaise(booking.getPricePaise())
                .build();

    }
}
