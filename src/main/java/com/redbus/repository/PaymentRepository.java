package com.redbus.repository;

import com.redbus.model.Payment;
import com.redbus.model.PaymentStatus;
import java.util.List;
import java.util.Optional;

public interface PaymentRepository {
    Optional<Payment> findById(String paymentId);
    Optional<Payment> findByBookingId(String bookingId);
    List<Payment> findByUserId(String userId);
    void save(Payment payment);
    void updateStatus(String paymentId, PaymentStatus status);
}
