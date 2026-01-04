package com.example.ordermanagement.listener;

import com.example.ordermanagement.config.RabbitConfig;
import com.example.ordermanagement.domain.JobLog;
import com.example.ordermanagement.domain.JobStatus;
import com.example.ordermanagement.domain.ReturnRequest;
import com.example.ordermanagement.messaging.dto.RefundJobDto;
import com.example.ordermanagement.repository.JobLogRepository;
import com.example.ordermanagement.repository.ReturnRequestRepository;
import com.example.ordermanagement.service.RefundClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Component
public class RefundListener {

    private static final Logger log = LoggerFactory.getLogger(RefundListener.class);

    private final RefundClient refundClient;
    private final JobLogRepository jobLogRepository;
    private final ReturnRequestRepository returnRequestRepository;

    public RefundListener(RefundClient refundClient, JobLogRepository jobLogRepository, ReturnRequestRepository returnRequestRepository) {
        this.refundClient = refundClient;
        this.jobLogRepository = jobLogRepository;
        this.returnRequestRepository = returnRequestRepository;
    }

    @RabbitListener(queues = RabbitConfig.REFUND_QUEUE)
    @Transactional
    public void handleRefund(RefundJobDto message) {
        UUID jobId = message.jobId;
        Optional<JobLog> jobOpt = jobLogRepository.findById(jobId);
        if (jobOpt.isEmpty()) {
            log.error("Unknown refund job: {}", jobId);
            return;
        }
        JobLog job = jobOpt.get();
        if (job.getStatus() == JobStatus.SUCCESS) {
            log.info("Refund already processed: {}", jobId);
            return;
        }

        job.setStatus(JobStatus.IN_PROGRESS);
        jobLogRepository.save(job);

        // load return request
        Optional<ReturnRequest> rrOpt = returnRequestRepository.findById(message.returnId);
        if (rrOpt.isEmpty()) {
            job.setStatus(JobStatus.FAILED);
            job.setLastError("ReturnRequest not found: " + message.returnId);
            jobLogRepository.save(job);
            return;
        }

        try {
            RefundClient.RefundRequestDto req = new RefundClient.RefundRequestDto();
            req.paymentReference = message.paymentReference;
            req.idempotencyKey = job.getIdempotencyKey();
            req.currency = message.currency;
            req.amountCents = 0L; // amount not tracked in ReturnRequest in this initial model

            RefundClient.RefundResponse resp = refundClient.processRefund(req);
            if (resp.success) {
                job.setStatus(JobStatus.SUCCESS);
                job.setResultMeta("gatewayRef=" + resp.gatewayReference);
                // mark return request as COMPLETED
                ReturnRequest rr = rrOpt.get();
                rr.setStatus(com.example.ordermanagement.domain.ReturnStatus.COMPLETED);
                returnRequestRepository.save(rr);
            } else {
                job.setStatus(JobStatus.FAILED);
                job.setLastError(resp.message);
            }
        } catch (Exception e) {
            job.setStatus(JobStatus.FAILED);
            job.setLastError(e.getMessage());
        }

        jobLogRepository.save(job);
    }
}
