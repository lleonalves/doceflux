package com.doceflux.doceflux_api.finance.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "financial_transactions")
public class FinancialTransaction {

    private static final int MONEY_SCALE = 2;

    @Id
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10, updatable = false)
    private TransactionType type;

    @Column(nullable = false)
    private String description;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(name = "transaction_date", nullable = false)
    private LocalDate transactionDate;

    @Column(length = 60)
    private String category;

    @Column(name = "order_id", updatable = false)
    private UUID orderId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected FinancialTransaction() {
    }

    private FinancialTransaction(TransactionType type, String description, BigDecimal amount,
                                 LocalDate transactionDate, String category, UUID orderId) {
        this.id = UUID.randomUUID();
        this.type = type;
        this.description = description;
        this.amount = amount.setScale(MONEY_SCALE);
        this.transactionDate = transactionDate;
        this.category = category;
        this.orderId = orderId;
        this.createdAt = Instant.now();
        this.updatedAt = this.createdAt;
    }

    public static FinancialTransaction income(String description, BigDecimal amount, LocalDate date,
                                              String category, UUID orderId) {
        return new FinancialTransaction(TransactionType.INCOME, description, amount, date, category, orderId);
    }

    public static FinancialTransaction expense(String description, BigDecimal amount, LocalDate date,
                                               String category) {
        return new FinancialTransaction(TransactionType.EXPENSE, description, amount, date, category, null);
    }

    public UUID getId() {
        return id;
    }

    public TransactionType getType() {
        return type;
    }

    public String getDescription() {
        return description;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public LocalDate getTransactionDate() {
        return transactionDate;
    }

    public String getCategory() {
        return category;
    }

    public UUID getOrderId() {
        return orderId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
