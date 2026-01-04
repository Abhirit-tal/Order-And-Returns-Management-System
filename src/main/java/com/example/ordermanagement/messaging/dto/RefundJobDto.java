package com.example.ordermanagement.messaging.dto;

import java.util.UUID;

public class RefundJobDto {
    public UUID jobId;
    public UUID orderId;
    public UUID returnId;
    public String paymentReference;
    public String currency;

    public RefundJobDto() {
    }

    public RefundJobDto(UUID jobId, UUID orderId, UUID returnId, String paymentReference, String currency) {
        this.jobId = jobId;
        this.orderId = orderId;
        this.returnId = returnId;
        this.paymentReference = paymentReference;
        this.currency = currency;
    }
}

