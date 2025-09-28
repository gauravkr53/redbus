package com.redbus.repository;

import com.redbus.model.Booking;
import com.redbus.model.BookingStatus;
import java.util.List;
import java.util.Optional;

public interface BookingRepository {
    Optional<Booking> findById(String bookingId);
    List<Booking> findByUserId(String userId);
    List<Booking> findByUserIdAndStatus(String userId, BookingStatus status);
    void save(Booking booking);
    void updateStatus(String bookingId, BookingStatus status);
    List<Booking> findExpiredReservations();
    void deleteExpiredReservations();
}
