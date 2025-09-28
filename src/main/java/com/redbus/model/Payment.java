package com.redbus.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Payment {
    private String paymentId;
    private String bookingId;
    private String userId;
    private long amountPaise;
    private PaymentStatus status;
    private String method;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
