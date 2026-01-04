package com.example.ordermanagement.listener;

import com.example.ordermanagement.config.RabbitConfig;
import com.example.ordermanagement.domain.JobLog;
import com.example.ordermanagement.domain.JobStatus;
import com.example.ordermanagement.messaging.dto.InvoiceJobDto;
import com.example.ordermanagement.repository.JobLogRepository;
import com.example.ordermanagement.service.PdfService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.UUID;

@Component
public class InvoiceListener {

    private static final Logger log = LoggerFactory.getLogger(InvoiceListener.class);

    private final PdfService pdfService;
    private final JobLogRepository jobLogRepository;

    public InvoiceListener(PdfService pdfService, JobLogRepository jobLogRepository) {
        this.pdfService = pdfService;
        this.jobLogRepository = jobLogRepository;
    }

    @RabbitListener(queues = RabbitConfig.INVOICE_QUEUE)
    public void handleInvoice(InvoiceJobDto message) {
        try {
            UUID jobId = message.jobId;
            UUID orderId = message.orderId;
            String customerEmail = message.customerEmail;

            JobLog job = jobLogRepository.findById(jobId).orElse(null);
            if (job != null && job.getStatus() == JobStatus.SUCCESS) {
                // already processed
                log.info("Invoice job already processed: {}", jobId);
                return;
            }

            if (job != null) {
                job.setStatus(JobStatus.IN_PROGRESS);
                jobLogRepository.save(job);
            }

            // generate pdf
            try {
                byte[] pdf = pdfService.generateInvoicePdf(orderId, customerEmail);
                if (job != null) {
                    job.setStatus(JobStatus.SUCCESS);
                    job.setResultMeta("pdfBytes=" + pdf.length);
                    jobLogRepository.save(job);
                }
                log.info("Generated invoice for order {} (bytes={})", orderId, pdf.length);
            } catch (IOException e) {
                if (job != null) {
                    job.setStatus(JobStatus.FAILED);
                    job.setLastError(e.getMessage());
                    jobLogRepository.save(job);
                }
                log.error("Failed to generate invoice for job {}: {}", jobId, e.getMessage(), e);
            }

        } catch (Exception ex) {
            log.error("Unhandled exception in InvoiceListener", ex);
        }
    }
}
