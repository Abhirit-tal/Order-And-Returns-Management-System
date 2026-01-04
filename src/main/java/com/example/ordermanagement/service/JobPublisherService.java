package com.example.ordermanagement.service;

import com.example.ordermanagement.domain.*;
import com.example.ordermanagement.messaging.dto.InvoiceJobDto;
import com.example.ordermanagement.messaging.dto.RefundJobDto;
import com.example.ordermanagement.repository.JobLogRepository;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class JobPublisherService {

    private final RabbitTemplate rabbitTemplate;
    private final JobLogRepository jobLogRepository;

    public JobPublisherService(RabbitTemplate rabbitTemplate, JobLogRepository jobLogRepository) {
        this.rabbitTemplate = rabbitTemplate;
        this.jobLogRepository = jobLogRepository;
    }

    public UUID publishInvoiceJob(Order order) {
        UUID jobId = UUID.randomUUID();
        JobLog job = new JobLog(jobId, JobType.INVOICE_GENERATION, order.getId(), null, jobId.toString(), JobStatus.PENDING);
        jobLogRepository.save(job);

        InvoiceJobDto dto = new InvoiceJobDto(jobId, order.getId(), order.getCustomerEmail());
        rabbitTemplate.convertAndSend("ordermanagement.jobs", "invoice.generate", dto);
        return jobId;
    }

    public UUID publishRefundJob(ReturnRequest returnRequest, String paymentReference, String currency) {
        UUID jobId = UUID.randomUUID();
        JobLog job = new JobLog(jobId, JobType.REFUND_PROCESSING, returnRequest.getOrder().getId(), returnRequest.getId(), jobId.toString(), JobStatus.PENDING);
        jobLogRepository.save(job);

        RefundJobDto dto = new RefundJobDto(jobId, returnRequest.getOrder().getId(), returnRequest.getId(), paymentReference, currency);
        rabbitTemplate.convertAndSend("ordermanagement.jobs", "refund.process", dto);
        return jobId;
    }
}
