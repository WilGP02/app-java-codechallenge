package com.yape.reto.tecnico.antifraud.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yape.reto.tecnico.antifraud.model.AntifraudResponse;
import com.yape.reto.tecnico.antifraud.model.Transaction;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.kafka.receiver.KafkaReceiver;
import reactor.kafka.receiver.ReceiverOptions;
import reactor.kafka.sender.KafkaSender;
import reactor.kafka.sender.SenderOptions;
import reactor.kafka.sender.SenderRecord;

import java.util.HashMap;
import java.util.Map;

@Service
public class AntifraudService {

    private final KafkaReceiver<String, String> kafkaReceiver;
    private final KafkaSender<String, String> kafkaSender;
    private final String inputTopicName = "transaction-created";
    private final String outputTopicName = "antifraud-responses";
    private final ObjectMapper objectMapper = new ObjectMapper();

    public AntifraudService() {
        Map<String, Object> props = new HashMap<>();
        props.put("bootstrap.servers", "localhost:9092");
        props.put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
        props.put("value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
        props.put("auto.offset.reset", "earliest");
        props.put("group.id", "antifraud-group");

        ReceiverOptions<String, String> receiverOptions = ReceiverOptions.create(props);
        this.kafkaReceiver = KafkaReceiver.create(receiverOptions.subscription(java.util.Collections.singleton(inputTopicName)));

        props.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        props.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        SenderOptions<String, String> senderOptions = SenderOptions.create(props);
        this.kafkaSender = KafkaSender.create(senderOptions);

        this.kafkaReceiver.receive()
                .flatMap(record -> {
                    Transaction transaction = deserializeTransaction(record.value());
                    return analyzeTransaction(transaction)
                            .then(record.receiverOffset().commit());
                })
                .doOnError(e -> {
                    System.err.println("Error processing transaction: " + e.getMessage());
                })
                .subscribe();
    }

    private Transaction deserializeTransaction(String json) {
        try {
            return objectMapper.readValue(json, Transaction.class);
        } catch (Exception e) {
            throw new RuntimeException("Failed to deserialize transaction", e);
        }
    }

    private Mono<Void> analyzeTransaction(Transaction transaction) {
        boolean isFraudulent = transaction.getAmount() > 1000;

        AntifraudResponse response = new AntifraudResponse();
        response.setTransactionId(transaction.getId());
        response.setFraudulent(isFraudulent);

        return publishAntifraudResponse(response);
    }

    private Mono<Void> publishAntifraudResponse(AntifraudResponse response) {
        return Mono.just(response)
                .map(r -> {
                    try {
                        return objectMapper.writeValueAsString(r);
                    } catch (Exception e) {
                        throw new RuntimeException("Failed to serialize antifraud response", e);
                    }
                })
                .flatMap(jsonResponse -> {
                    SenderRecord<String, String, String> senderRecord = SenderRecord.create(outputTopicName, null, null, response.getTransactionId().toString(), jsonResponse, null);
                    return kafkaSender.send(Mono.just(senderRecord)).then();
                });
    }
}