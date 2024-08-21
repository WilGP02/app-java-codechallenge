package com.yape.reto.tecnico.kafka.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yape.reto.tecnico.kafka.model.AntifraudResponse;
import com.yape.reto.tecnico.kafka.model.Transaction;
import com.yape.reto.tecnico.kafka.repository.TransactionRepository;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.kafka.receiver.KafkaReceiver;
import reactor.kafka.receiver.ReceiverOptions;
import reactor.kafka.sender.KafkaSender;
import reactor.kafka.sender.SenderOptions;
import reactor.kafka.sender.SenderRecord;

import java.util.HashMap;
import java.util.Map;

@Service
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final KafkaSender<String, String> kafkaSender;
    private final KafkaReceiver<String, String> kafkaReceiver;
    private final String topicName = "transaction-created";
    private final ObjectMapper objectMapper = new ObjectMapper();

    public TransactionService(TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;

        Map<String, Object> props = new HashMap<>();
        props.put("bootstrap.servers", "localhost:9092");
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

    public Mono<Transaction> createTransaction(int amount) {
        Transaction transaction = new Transaction(amount, "PENDING");
        Transaction savedTransaction = transactionRepository.save(transaction);

        return publishTransactionEvent(savedTransaction).thenReturn(savedTransaction);
    }

    private Mono<Void> handleAntifraudResponse(AntifraudResponse response) {
        return Mono.just(response)
                .flatMap(r -> {
                    return Mono.justOrEmpty(transactionRepository.findById(r.getTransactionId()))
                            .flatMap(transaction -> {
                                if (r.isFraudulent()) {
                                    transaction.setStatus("REJECTED");
                                } else {
                                    transaction.setStatus("APPROVED");
                                }
                                transactionRepository.save(transaction);
                                return Mono.empty();
                            });
                });
    }

    public Mono<Transaction> getTransaction(Long id) {
        return Mono.justOrEmpty(transactionRepository.findById(id));
    }

    public Flux<Transaction> getAllTransactions() {
        return Flux.fromIterable(transactionRepository.findAll());
    }

    private Mono<Void> publishTransactionEvent(Transaction transaction) {
        return Mono.just(transaction)
                .map(t -> {
                    try {
                        return objectMapper.writeValueAsString(t);
                    } catch (Exception e) {
                        throw new RuntimeException("Failed to serialize transaction", e);
                    }
                })
                .flatMap(jsonTransaction -> {
                    SenderRecord<String, String, String> senderRecord = SenderRecord.create(topicName, null, null, transaction.getId().toString(), jsonTransaction, null);
                    return kafkaSender.send(Mono.just(senderRecord)).then();
                });
    }
}
