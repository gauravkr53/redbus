package com.redbus.controller;

import com.redbus.dto.BookingResponse;
import com.redbus.dto.CreateBookingRequest;
import com.redbus.dto.BookingView;
import com.redbus.model.Booking;
import com.redbus.pricing.PricingService;
import com.redbus.repository.BookingRepository;
import com.redbus.repository.CityRepository;
import com.redbus.service.AuthService;
import com.redbus.service.IdempotencyStore;
import com.redbus.service.InventoryService;
import com.redbus.service.OrderService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/v1/bookings")
@RequiredArgsConstructor
public class BookingController {
    private final OrderService orderService;
    private final AuthService authService;
    private final InventoryService inventoryService;
    private final CityRepository cityRepository;
    private final PricingService pricingService;
    private final IdempotencyStore idempotencyStore;
    private final BookingRepository bookingRepository;

    @PostMapping
    public ResponseEntity<BookingResponse> createBooking(
            @Valid @RequestBody CreateBookingRequest request,
            @RequestHeader("Authorization") String authHeader,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            HttpServletRequest httpRequest) {
        
        String userId = authService.extractUserId(authHeader);
        
        // Create idempotency key
        String idempotencyKeyFull = idempotencyKey + "_" + userId + "_" + 
                (request.getTripId() + "_" + request.getSourceCityId() + "_" + request.getDestCityId() + "_" + request.getSeats()).hashCode();
        
        // Check for existing result
        Object existingResult = idempotencyStore.getResult(idempotencyKeyFull);
        if (existingResult != null) {
            BookingResponse response = (BookingResponse) existingResult;
            return ResponseEntity.ok(response);
        }
        
        // Create booking
        Booking booking = orderService.createBooking(userId, request.getTripId(), request.getSourceCityId(), request.getDestCityId(), request.getSeats());
        
        // Calculate actual price
        var trip = inventoryService.getTrip(request.getTripId());
        if (trip.isPresent()) {
            var sourceCity = cityRepository.findById(trip.get().getSourceCityId());
            var destCity = cityRepository.findById(trip.get().getDestCityId());
            
            if (sourceCity.isPresent() && destCity.isPresent()) {
                long price = pricingService.calculatePrice(trip.get(), sourceCity.get(), destCity.get(), request.getSeats(), 0, 0);
                booking.setPricePaise(price);
            }
        }
        
        BookingResponse response = new BookingResponse(booking.getBookingId(), booking.getStatus());
        
        // Store result for idempotency (expires in 24 hours)
        idempotencyStore.storeResult(idempotencyKeyFull, response, LocalDateTime.now().plusHours(24));
        
        return ResponseEntity.ok(response);
    }

    @GetMapping("/all-bookings")
    public ResponseEntity<List<BookingView>> getAllBookings(
            @RequestHeader("Authorization") String authHeader) {

        String userId = authService.extractUserId(authHeader);

        List<BookingView> allBookings = orderService.getAllBookings(userId);

        return ResponseEntity.ok(allBookings);
    }
}
