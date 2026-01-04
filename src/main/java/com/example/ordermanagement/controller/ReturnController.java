package com.example.ordermanagement.controller;

import com.example.ordermanagement.domain.ReturnRequest;
import com.example.ordermanagement.domain.ReturnStatus;
import com.example.ordermanagement.service.JobPublisherService;
import com.example.ordermanagement.service.ReturnService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/returns")
public class ReturnController {

    private final ReturnService returnService;
    private final JobPublisherService jobPublisherService;

    public ReturnController(ReturnService returnService, JobPublisherService jobPublisherService) {
        this.returnService = returnService;
        this.jobPublisherService = jobPublisherService;
    }

    @PostMapping
    public ResponseEntity<ReturnRequest> createReturn(@RequestBody Map<String, String> body) {
        UUID orderId = UUID.fromString(body.get("orderId"));
        String reason = body.getOrDefault("reason", "no reason");
        ReturnRequest rr = returnService.createReturn(orderId, reason);
        return ResponseEntity.ok(rr);
    }

    @PostMapping("/{id}/status")
    public ResponseEntity<ReturnRequest> changeStatus(@PathVariable("id") UUID id, @RequestBody Map<String, String> body) {
        ReturnStatus status = ReturnStatus.valueOf(body.get("status"));
        ReturnRequest rr = returnService.changeReturnStatus(id, status, "api", "manual");

        // if status becomes COMPLETED, publish refund job (dummy paymentRef/currency for now)
        if (rr.getStatus() == ReturnStatus.COMPLETED) {
            jobPublisherService.publishRefundJob(rr, "ORIG-PAYMENT-REF", "USD");
        }

        return ResponseEntity.ok(rr);
    }
}

