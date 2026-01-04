package com.example.ordermanagement.service;

import com.example.ordermanagement.domain.ReturnRequest;

public interface RefundClient {
    RefundResponse processRefund(RefundRequestDto request) throws RefundException;

    class RefundRequestDto {
        public String paymentReference;
        public String idempotencyKey;
        public String currency;
        public long amountCents;
    }

    class RefundResponse {
        public boolean success;
        public String gatewayReference;
        public String message;
    }

    class RefundException extends Exception {
        public RefundException(String message) {
            super(message);
        }

        public RefundException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}

