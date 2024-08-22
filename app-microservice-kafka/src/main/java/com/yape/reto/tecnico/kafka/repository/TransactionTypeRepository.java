package com.yape.reto.tecnico.kafka.repository;

import com.yape.reto.tecnico.kafka.entity.TransactionType;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TransactionTypeRepository extends JpaRepository<TransactionType, Long> {
}