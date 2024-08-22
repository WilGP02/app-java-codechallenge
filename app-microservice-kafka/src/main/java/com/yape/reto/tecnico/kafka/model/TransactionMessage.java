package com.yape.reto.tecnico.kafka.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
public class TransactionMessage {

    private UUID transactionExternalId;
    private TransactionTypeDto transactionType;
    private TransactionStatusDto  transactionStatus;
    private Double amount;
    private LocalDateTime createdAt;

}
