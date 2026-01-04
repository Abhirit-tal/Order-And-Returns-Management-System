package com.example.ordermanagement.repository;

import com.example.ordermanagement.domain.JobLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface JobLogRepository extends JpaRepository<JobLog, UUID> {
    Optional<JobLog> findByIdempotencyKey(String idempotencyKey);
}

