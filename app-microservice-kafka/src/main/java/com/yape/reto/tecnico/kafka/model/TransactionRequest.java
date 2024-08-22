package com.yape.reto.tecnico.kafka.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
public class TransactionRequest {

    private String accountExternalIdDebit ;
    private String accountExternalIdCredit;
    private Long tranferTypeId ;
    private Double amount;

}
