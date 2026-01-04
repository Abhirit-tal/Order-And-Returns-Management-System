package com.example.ordermanagement.messaging.dto;

import java.util.UUID;

public class InvoiceJobDto {
    public UUID jobId;
    public UUID orderId;
    public String customerEmail;

    public InvoiceJobDto() {
    }

    public InvoiceJobDto(UUID jobId, UUID orderId, String customerEmail) {
        this.jobId = jobId;
        this.orderId = orderId;
        this.customerEmail = customerEmail;
    }
}

