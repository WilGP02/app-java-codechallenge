package com.yape.reto.tecnico.kafka.repository;

import com.yape.reto.tecnico.kafka.entity.Transactions;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface TransactionRepository extends JpaRepository<Transactions, UUID> {


}
