package com.yape.reto.tecnico.kafka.repository;

import com.yape.reto.tecnico.kafka.model.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {


}
