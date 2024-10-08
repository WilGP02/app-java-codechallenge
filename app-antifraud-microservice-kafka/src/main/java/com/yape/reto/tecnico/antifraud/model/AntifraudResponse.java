package com.yape.reto.tecnico.antifraud.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class AntifraudResponse {
    private UUID transactionId;
    private boolean isFraudulent;
}