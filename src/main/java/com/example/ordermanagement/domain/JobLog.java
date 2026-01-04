package com.example.ordermanagement.domain;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "job_log")
public class JobLog {

    @Id
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(name = "job_type")
    private JobType jobType;

    @Column(name = "related_order_id")
    private UUID relatedOrderId;

    @Column(name = "related_return_id")
    private UUID relatedReturnId;

    @Column(name = "idempotency_key")
    private String idempotencyKey;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private JobStatus status;

    @Column(name = "attempts")
    private int attempts;

    @Column(name = "last_error", columnDefinition = "text")
    private String lastError;

    @Column(name = "result_meta", columnDefinition = "text")
    private String resultMeta;

    @Column(name = "created_at")
    private OffsetDateTime createdAt;

    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;

    public JobLog() {
        // JPA
    }

    public JobLog(UUID id, JobType jobType, UUID relatedOrderId, UUID relatedReturnId, String idempotencyKey, JobStatus status) {
        this.id = id;
        this.jobType = jobType;
        this.relatedOrderId = relatedOrderId;
        this.relatedReturnId = relatedReturnId;
        this.idempotencyKey = idempotencyKey;
        this.status = status;
        this.attempts = 0;
        this.createdAt = OffsetDateTime.now();
        this.updatedAt = OffsetDateTime.now();
    }

    // getters/setters

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public JobType getJobType() {
        return jobType;
    }

    public void setJobType(JobType jobType) {
        this.jobType = jobType;
    }

    public UUID getRelatedOrderId() {
        return relatedOrderId;
    }

    public void setRelatedOrderId(UUID relatedOrderId) {
        this.relatedOrderId = relatedOrderId;
    }

    public UUID getRelatedReturnId() {
        return relatedReturnId;
    }

    public void setRelatedReturnId(UUID relatedReturnId) {
        this.relatedReturnId = relatedReturnId;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public void setIdempotencyKey(String idempotencyKey) {
        this.idempotencyKey = idempotencyKey;
    }

    public JobStatus getStatus() {
        return status;
    }

    public void setStatus(JobStatus status) {
        this.status = status;
        this.updatedAt = OffsetDateTime.now();
    }

    public int getAttempts() {
        return attempts;
    }

    public void setAttempts(int attempts) {
        this.attempts = attempts;
    }

    public String getLastError() {
        return lastError;
    }

    public void setLastError(String lastError) {
        this.lastError = lastError;
    }

    public String getResultMeta() {
        return resultMeta;
    }

    public void setResultMeta(String resultMeta) {
        this.resultMeta = resultMeta;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(OffsetDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}

