package com.yape.reto.tecnico.kafka.service.graphql;

import com.yape.reto.tecnico.kafka.model.Transaction;
import com.yape.reto.tecnico.kafka.service.TransactionService;
import org.springframework.stereotype.Component;
import graphql.kickstart.tools.GraphQLQueryResolver;
import graphql.kickstart.tools.GraphQLMutationResolver;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

@Component
public class TransactionGraphQLResolver implements GraphQLQueryResolver, GraphQLMutationResolver {

    private final TransactionService transactionService;

    public TransactionGraphQLResolver(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    public Mono<Transaction> getTransactionById(Long id) {
        return transactionService.getTransaction(id);
    }

    public Mono<List<Transaction>> allTransactions() {
        return transactionService.getAllTransactions().collectList();
    }

    public Mono<Transaction> createTransaction(int amount) {
        return transactionService.createTransaction(amount);
    }
}
