package com.yape.reto.tecnico.kafka.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.yape.reto.tecnico.kafka.entity.Transactions;
import com.yape.reto.tecnico.kafka.model.AntifraudResponse;
import com.yape.reto.tecnico.kafka.model.TransactionMessage;
import com.yape.reto.tecnico.kafka.model.TransactionRequest;
import com.yape.reto.tecnico.kafka.entity.TransactionStatus;
import com.yape.reto.tecnico.kafka.entity.TransactionType;
import com.yape.reto.tecnico.kafka.model.TransactionStatusDto;
import com.yape.reto.tecnico.kafka.model.TransactionTypeDto;
import com.yape.reto.tecnico.kafka.repository.TransactionRepository;
import com.yape.reto.tecnico.kafka.repository.TransactionStatusRepository;
import com.yape.reto.tecnico.kafka.repository.TransactionTypeRepository;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.kafka.receiver.KafkaReceiver;
import reactor.kafka.receiver.ReceiverOptions;
import reactor.kafka.sender.KafkaSender;
import reactor.kafka.sender.SenderOptions;
import reactor.kafka.sender.SenderRecord;

import javax.transaction.Transactional;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Log4j2
@Service
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final TransactionStatusRepository transactionStatusRepository;
    private final TransactionTypeRepository transactionTypeRepository;
    private final KafkaSender<String, String> kafkaSender;
    private final KafkaReceiver<String, String> kafkaReceiver;
    private final String topicName = "transaction-created";
    private final ObjectMapper objectMapper = new ObjectMapper();


    public TransactionService(TransactionRepository transactionRepository, TransactionStatusRepository transactionStatusRepository,
                              TransactionTypeRepository transactionTypeRepository) {
        this.transactionRepository = transactionRepository;
        this.transactionStatusRepository = transactionStatusRepository;
        this.transactionTypeRepository = transactionTypeRepository;
        objectMapper.registerModule(new JavaTimeModule());

        Map<String, Object> props = new HashMap<>();
        props.put("bootstrap.servers", "kafka:29092");
        props.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        props.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        SenderOptions<String, String> senderOptions = SenderOptions.create(props);
        this.kafkaSender = KafkaSender.create(senderOptions);

        props.put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
        props.put("value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
        props.put("group.id", "transaction-group");
        ReceiverOptions<String, String> receiverOptions = ReceiverOptions.create(props);
        this.kafkaReceiver = KafkaReceiver.create(receiverOptions.subscription(java.util.Collections.singleton("antifraud-responses")));

        this.kafkaReceiver.receive()
                .flatMap(record -> {
                    AntifraudResponse response = deserializeAntifraudResponse(record.value());
                    return handleAntifraudResponse(response);
                })
                .subscribe();
    }

    private AntifraudResponse deserializeAntifraudResponse(String json) {
        try {
            return objectMapper.readValue(json, AntifraudResponse.class);
        } catch (Exception e) {
            throw new RuntimeException("Failed to deserialize antifraud response", e);
        }
    }

    @Transactional
    public Mono<Transactions> createTransaction(TransactionRequest request) {
        TransactionStatus transactionStatus = transactionStatusRepository.findById(1L).get();
        TransactionType transactionType = transactionTypeRepository.findById(request.getTranferTypeId()).get();

        Transactions savedTransactions = transactionRepository.save(Transactions
                .builder()
                        .accountExternalIdCredit(request.getAccountExternalIdCredit())
                        .accountExternalIdDebit(request.getAccountExternalIdDebit())
                        .transactionStatus(transactionStatus)
                        .transactionType(transactionType)
                        .amount(request.getAmount())
                .build());

        TransactionMessage transactionMessage = TransactionMessage
                .builder()
                .transactionExternalId(savedTransactions.getId())
                .transactionStatus(TransactionStatusDto.builder().name(savedTransactions.getTransactionStatus().getName()).build())
                .transactionType(TransactionTypeDto.builder().name(savedTransactions.getTransactionType().getName()).build())
                .amount(savedTransactions.getAmount())
                .createdAt(LocalDateTime.now())
                .build();

        return publishTransactionEvent(transactionMessage)
                .thenReturn(savedTransactions);
    }

    private Mono<Void> handleAntifraudResponse(AntifraudResponse response) {
        return Mono.just(response)
                .flatMap(r -> {
                    return Mono.justOrEmpty(transactionRepository.findById(r.getTransactionId()))
                            .flatMap(transactions -> {
                                if (r.isFraudulent()) {
                                    TransactionStatus transactionStatus = transactionStatusRepository.findById(3L).get();
                                    transactions.setTransactionStatus(transactionStatus);
                                } else {
                                    TransactionStatus transactionStatus = transactionStatusRepository.findById(2L).get();
                                    transactions.setTransactionStatus(transactionStatus);
                                }
                                transactionRepository.save(transactions);
                                return Mono.empty();
                            });
                });
    }

    public Mono<Transactions> getTransaction(UUID id) {
        return Mono.justOrEmpty(transactionRepository.findById(id));
    }

    public Flux<Transactions> getAllTransactions() {
        return Flux.fromIterable(transactionRepository.findAll());
    }

    private Mono<Void> publishTransactionEvent(TransactionMessage transactionMessage) {
        return Mono.just(transactionMessage)
                .map(t -> {
                    try {
                        log.info("1Publicación exitosa de transactionMessage: {} ",  objectMapper.writeValueAsString(t));
                        return objectMapper.writeValueAsString(t);
                    } catch (Exception e) {
                        throw new RuntimeException("Failed to serialize transaction", e);
                    }
                })
                .flatMap(jsonTransaction -> {
                    log.info("2Publicación exitosa de transactionMessage: {} ",  jsonTransaction);
                    SenderRecord<String, String, String> senderRecord = SenderRecord.create(topicName, null,
                            null, transactionMessage.getTransactionExternalId().toString(), jsonTransaction, null);
                    return kafkaSender.send(Mono.just(senderRecord)).then();
                });
    }
}
