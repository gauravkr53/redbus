package com.redbus.repository.impl;

import com.redbus.model.Payment;
import com.redbus.model.PaymentStatus;
import com.redbus.repository.PaymentRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Repository
public class InMemoryPaymentRepository implements PaymentRepository {
    private final Map<String, Payment> payments = new ConcurrentHashMap<>();

    @Override
    public Optional<Payment> findById(String paymentId) {
        return Optional.ofNullable(payments.get(paymentId));
    }

    @Override
    public Optional<Payment> findByBookingId(String bookingId) {
        return payments.values().stream()
                .filter(payment -> payment.getBookingId().equals(bookingId))
                .findFirst();
    }

    @Override
    public List<Payment> findByUserId(String userId) {
        return payments.values().stream()
                .filter(payment -> payment.getUserId().equals(userId))
                .collect(Collectors.toList());
    }

    @Override
    public void save(Payment payment) {
        payments.put(payment.getPaymentId(), payment);
    }

    @Override
    public void updateStatus(String paymentId, PaymentStatus status) {
        Payment payment = payments.get(paymentId);
        if (payment != null) {
            payment.setStatus(status);
            payment.setUpdatedAt(java.time.LocalDateTime.now());
        }
    }
}
