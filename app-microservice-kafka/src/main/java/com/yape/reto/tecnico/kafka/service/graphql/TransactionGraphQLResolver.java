package com.yape.reto.tecnico.kafka.service.graphql;

import com.yape.reto.tecnico.kafka.entity.Transactions;
import com.yape.reto.tecnico.kafka.model.TransactionRequest;
import com.yape.reto.tecnico.kafka.service.TransactionService;
import org.springframework.stereotype.Component;
import graphql.kickstart.tools.GraphQLQueryResolver;
import graphql.kickstart.tools.GraphQLMutationResolver;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.UUID;

@Component
public class TransactionGraphQLResolver implements GraphQLQueryResolver, GraphQLMutationResolver {

    private final TransactionService transactionService;

    public TransactionGraphQLResolver(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    public Mono<Transactions> getTransactionById(UUID id) {
        return transactionService.getTransaction(id);
    }

    public Mono<List<Transactions>> allTransactions() {
        return transactionService.getAllTransactions().collectList();
    }

    public Mono<Transactions> createTransaction(TransactionRequest request) {
        return transactionService.createTransaction(request);
    }
}
