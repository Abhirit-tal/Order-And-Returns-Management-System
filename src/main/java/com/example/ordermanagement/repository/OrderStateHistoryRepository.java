package com.example.ordermanagement.repository;

import com.example.ordermanagement.domain.OrderStateHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface OrderStateHistoryRepository extends JpaRepository<OrderStateHistory, UUID> {
}

