package com.doceflux.doceflux_api.order.domain;

import com.doceflux.doceflux_api.customer.domain.Customer;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import com.doceflux.doceflux_api.shared.BusinessRuleException;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "orders")
public class Order {

    private static final int MONEY_SCALE = 2;
    private static final int MAX_TOTAL_PRECISION = 12;

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id", nullable = false, updatable = false)
    private Customer customer;

    @Column(columnDefinition = "TEXT")
    private String description;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL)
    private List<OrderItem> items = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OrderStatus status;

    @Column(name = "total_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Order() {
    }

    public Order(Customer customer, String description) {
        this.id = UUID.randomUUID();
        this.customer = customer;
        this.description = description;
        this.status = OrderStatus.PENDING;
        this.totalAmount = BigDecimal.ZERO.setScale(MONEY_SCALE);
        this.createdAt = Instant.now();
        this.updatedAt = this.createdAt;
    }

    public void addItem(OrderItem item) {
        BigDecimal newTotal = totalAmount.add(item.getSubtotal());
        if (newTotal.precision() > MAX_TOTAL_PRECISION) {
            throw new BusinessRuleException("O valor total do pedido excede o limite permitido");
        }
        item.attachTo(this);
        items.add(item);
        totalAmount = newTotal;
    }

    public void changeStatus(OrderStatus next) {
        if (next == null) {
            throw new BusinessRuleException("O status é obrigatório");
        }
        if (!status.canTransitionTo(next)) {
            throw new BusinessRuleException(
                    "Transição de status inválida: %s -> %s".formatted(status, next));
        }
        this.status = next;
        this.updatedAt = Instant.now();
    }

    public OrderStatus getStatus() {
        return status;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public UUID getId() {
        return id;
    }

    public Customer getCustomer() {
        return customer;
    }

    public String getDescription() {
        return description;
    }

    public List<OrderItem> getItems() {
        return Collections.unmodifiableList(items);
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
