package com.redbus.repository.impl;

import com.redbus.model.Booking;
import com.redbus.model.BookingStatus;
import com.redbus.repository.BookingRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Repository
public class InMemoryBookingRepository implements BookingRepository {
    private final Map<String, Booking> bookings = new ConcurrentHashMap<>();

    @Override
    public Optional<Booking> findById(String bookingId) {
        return Optional.ofNullable(bookings.get(bookingId));
    }

    @Override
    public List<Booking> findByUserId(String userId) {
        return bookings.values().stream()
                .filter(booking -> booking.getUserId().equals(userId))
                .collect(Collectors.toList());
    }

    @Override
    public List<Booking> findByUserIdAndStatus(String userId, BookingStatus status) {
        return bookings.values().stream()
                .filter(booking -> booking.getUserId().equals(userId) && booking.getStatus() == status)
                .collect(Collectors.toList());
    }

    @Override
    public void save(Booking booking) {
        bookings.put(booking.getBookingId(), booking);
    }

    @Override
    public void updateStatus(String bookingId, BookingStatus status) {
        Booking booking = bookings.get(bookingId);
        if (booking != null) {
            booking.setStatus(status);
        }
    }

    @Override
    public List<Booking> findExpiredReservations() {
        LocalDateTime now = LocalDateTime.now();
        return bookings.values().stream()
                .filter(booking -> booking.getStatus() == BookingStatus.RESERVED 
                        && booking.getExpiresAt() != null 
                        && booking.getExpiresAt().isBefore(now))
                .collect(Collectors.toList());
    }

    @Override
    public void deleteExpiredReservations() {
        List<Booking> expiredBookings = findExpiredReservations();
        expiredBookings.forEach(booking -> bookings.remove(booking.getBookingId()));
    }
}
