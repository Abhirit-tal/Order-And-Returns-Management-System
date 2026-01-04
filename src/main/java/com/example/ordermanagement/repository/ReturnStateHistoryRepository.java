package com.example.ordermanagement.repository;

import com.example.ordermanagement.domain.ReturnStateHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface ReturnStateHistoryRepository extends JpaRepository<ReturnStateHistory, UUID> {
}

