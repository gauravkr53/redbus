package com.redbus.controller;

import com.redbus.dto.CreatePaymentRequest;
import com.redbus.dto.PaymentResponse;
import com.redbus.model.Booking;
import com.redbus.model.Payment;
import com.redbus.repository.BookingRepository;
import com.redbus.service.AuthService;
import com.redbus.service.IdempotencyStore;
import com.redbus.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Optional;

@RestController
@RequestMapping("/v1/payments")
@RequiredArgsConstructor
public class PaymentController {
    private final PaymentService paymentService;
    private final AuthService authService;
    private final BookingRepository bookingRepository;
    private final IdempotencyStore idempotencyStore;

    @PostMapping
    public ResponseEntity<PaymentResponse> createPayment(
            @Valid @RequestBody CreatePaymentRequest request,
            @RequestHeader("Authorization") String authHeader,
            @RequestHeader("Idempotency-Key") String idempotencyKey) {
        
        String userId = authService.extractUserId(authHeader);
        
        // Create idempotency key
        String idempotencyKeyFull = idempotencyKey + "_" + userId + "_" + 
                (request.getBookingId() + "_" + request.getMethod()).hashCode();
        
        // Check for existing result
        Object existingResult = idempotencyStore.getResult(idempotencyKeyFull);
        if (existingResult != null) {
            PaymentResponse response = (PaymentResponse) existingResult;
            return ResponseEntity.ok(response);
        }
        
        // Validate booking
        Optional<Booking> booking = bookingRepository.findById(request.getBookingId());
        if (booking.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        
        if (!booking.get().getUserId().equals(userId)) {
            return ResponseEntity.status(403).build();
        }
        
        if (booking.get().getStatus() != com.redbus.model.BookingStatus.RESERVED) {
            return ResponseEntity.badRequest().build();
        }
        
        // Create payment
        Payment payment = paymentService.initiatePayment(
                request.getBookingId(),
                userId,
                booking.get().getPricePaise(),
                request.getMethod()
        );
        
        PaymentResponse response = new PaymentResponse(payment.getPaymentId(), payment.getStatus());
        
        // Store result for idempotency (expires in 24 hours)
        idempotencyStore.storeResult(idempotencyKeyFull, response, LocalDateTime.now().plusHours(24));
        
        return ResponseEntity.ok(response);
    }
}
