package com.example.ordermanagement.service;

import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class MockRefundClient implements RefundClient {

    @Override
    public RefundResponse processRefund(RefundRequestDto request) throws RefundException {
        // simulate processing delay
        try {
            Thread.sleep(200);
        } catch (InterruptedException ignored) {
        }
        RefundResponse r = new RefundResponse();
        r.success = true;
        r.gatewayReference = "MOCK-REFUND-" + UUID.randomUUID();
        r.message = "Mock refund successful";
        return r;
    }
}

