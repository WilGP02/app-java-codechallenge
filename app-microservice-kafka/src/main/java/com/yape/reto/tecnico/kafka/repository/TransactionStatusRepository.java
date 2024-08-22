package com.yape.reto.tecnico.kafka.repository;

import com.yape.reto.tecnico.kafka.entity.TransactionStatus;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TransactionStatusRepository extends JpaRepository<TransactionStatus, Long> {
}