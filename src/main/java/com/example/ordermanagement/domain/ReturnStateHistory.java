package com.example.ordermanagement.domain;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "return_state_history")
public class ReturnStateHistory {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "return_request_id", nullable = false)
    private ReturnRequest returnRequest;

    @Enumerated(EnumType.STRING)
    @Column(name = "from_state")
    private ReturnStatus fromState;

    @Enumerated(EnumType.STRING)
    @Column(name = "to_state")
    private ReturnStatus toState;

    @Column(name = "changed_by")
    private String changedBy;

    @Column(name = "reason")
    private String reason;

    @Column(name = "created_at")
    private OffsetDateTime createdAt;

    public ReturnStateHistory() {
        // JPA
    }

    public ReturnStateHistory(UUID id, ReturnRequest returnRequest, ReturnStatus fromState, ReturnStatus toState, String changedBy, String reason) {
        this.id = id;
        this.returnRequest = returnRequest;
        this.fromState = fromState;
        this.toState = toState;
        this.changedBy = changedBy;
        this.reason = reason;
        this.createdAt = OffsetDateTime.now();
    }

    // getters/setters

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public ReturnRequest getReturnRequest() {
        return returnRequest;
    }

    public void setReturnRequest(ReturnRequest returnRequest) {
        this.returnRequest = returnRequest;
    }

    public ReturnStatus getFromState() {
        return fromState;
    }

    public void setFromState(ReturnStatus fromState) {
        this.fromState = fromState;
    }

    public ReturnStatus getToState() {
        return toState;
    }

    public void setToState(ReturnStatus toState) {
        this.toState = toState;
    }

    public String getChangedBy() {
        return changedBy;
    }

    public void setChangedBy(String changedBy) {
        this.changedBy = changedBy;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }
}

