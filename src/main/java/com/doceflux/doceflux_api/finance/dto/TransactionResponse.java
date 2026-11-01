package com.doceflux.doceflux_api.finance.dto;

import com.doceflux.doceflux_api.finance.domain.FinancialTransaction;
import com.doceflux.doceflux_api.finance.domain.TransactionType;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record TransactionResponse(
        UUID id,
        TransactionType type,
        String description,
        BigDecimal amount,
        LocalDate date,
        String category,
        UUID orderId,
        Instant createdAt,
        Instant updatedAt) {

    public static TransactionResponse from(FinancialTransaction transaction) {
        return new TransactionResponse(
                transaction.getId(),
                transaction.getType(),
                transaction.getDescription(),
                transaction.getAmount(),
                transaction.getTransactionDate(),
                transaction.getCategory(),
                transaction.getOrderId(),
                transaction.getCreatedAt(),
                transaction.getUpdatedAt());
    }
}
