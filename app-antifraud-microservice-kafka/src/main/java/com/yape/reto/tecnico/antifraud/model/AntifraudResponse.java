package com.yape.reto.tecnico.antifraud.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class AntifraudResponse {
    private Long transactionId;
    private boolean isFraudulent;
}