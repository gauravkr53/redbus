package com.redbus.service;

import com.redbus.model.Payment;
import com.redbus.model.PaymentStatus;
import com.redbus.repository.PaymentRepository;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {
    private final PaymentRepository paymentRepository;
    private final OrderService orderService;

    public Payment initiatePayment(String bookingId, String userId, long amountPaise, String method) {
        Payment payment = Payment.builder()
                .paymentId(UUID.randomUUID().toString())
                .bookingId(bookingId)
                .userId(userId)
                .amountPaise(amountPaise)
                .status(PaymentStatus.INITIATED)
                .method(method)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        paymentRepository.save(payment);
        log.info("Initiated payment {} for booking {} with amount {} paise", 
                payment.getPaymentId(), bookingId, amountPaise);

        // Mock gateway processing
        processPayment(payment);

        return payment;
    }

    private void processPayment(Payment payment) {
        // Simulate gateway processing delay
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Mock gateway response - 90% success rate
        boolean success = ThreadLocalRandom.current().nextDouble() < 0.9;
        
        if (success) {
            payment.setStatus(PaymentStatus.SUCCESS);
            payment.setUpdatedAt(LocalDateTime.now());
            paymentRepository.updateStatus(payment.getPaymentId(), PaymentStatus.SUCCESS);
            
            // Confirm booking on successful payment
            orderService.confirmBooking(payment.getBookingId(), payment.getPaymentId());
            
            log.info("Payment {} succeeded for booking {}", payment.getPaymentId(), payment.getBookingId());
        } else {
            payment.setStatus(PaymentStatus.FAILED);
            payment.setUpdatedAt(LocalDateTime.now());
            paymentRepository.updateStatus(payment.getPaymentId(), PaymentStatus.FAILED);
            
            // Release booking on failed payment
            orderService.releaseBooking(payment.getBookingId());
            
            log.info("Payment {} failed for booking {}", payment.getPaymentId(), payment.getBookingId());
        }
    }
}
