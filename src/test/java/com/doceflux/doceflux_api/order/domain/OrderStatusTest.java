package com.doceflux.doceflux_api.order.domain;

import com.doceflux.doceflux_api.customer.domain.Customer;
import com.doceflux.doceflux_api.shared.BusinessRuleException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OrderStatusTest {

    private Order orderIn(OrderStatus target) {
        Order order = new Order(new Customer("Maria", "11999999999", null, null), null);
        switch (target) {
            case PENDING -> { }
            case IN_PRODUCTION -> order.changeStatus(OrderStatus.IN_PRODUCTION);
            case COMPLETED -> {
                order.changeStatus(OrderStatus.IN_PRODUCTION);
                order.changeStatus(OrderStatus.COMPLETED);
            }
            case CANCELLED -> order.changeStatus(OrderStatus.CANCELLED);
        }
        return order;
    }

    @Test
    void newOrderStartsPendingWithZeroTotal() {
        Order order = orderIn(OrderStatus.PENDING);

        assertThat(order.getStatus()).isEqualTo(OrderStatus.PENDING);
        assertThat(order.getTotalAmount()).isEqualByComparingTo("0.00");
    }

    @ParameterizedTest
    @CsvSource({
            "PENDING, IN_PRODUCTION",
            "PENDING, CANCELLED",
            "IN_PRODUCTION, COMPLETED",
            "IN_PRODUCTION, CANCELLED"
    })
    void allowsValidTransitions(OrderStatus from, OrderStatus to) {
        Order order = orderIn(from);

        order.changeStatus(to);

        assertThat(order.getStatus()).isEqualTo(to);
    }

    @ParameterizedTest
    @CsvSource({
            "PENDING, PENDING",
            "PENDING, COMPLETED",
            "IN_PRODUCTION, PENDING",
            "IN_PRODUCTION, IN_PRODUCTION",
            "COMPLETED, PENDING",
            "COMPLETED, IN_PRODUCTION",
            "COMPLETED, COMPLETED",
            "COMPLETED, CANCELLED",
            "CANCELLED, PENDING",
            "CANCELLED, IN_PRODUCTION",
            "CANCELLED, COMPLETED",
            "CANCELLED, CANCELLED"
    })
    void rejectsInvalidTransitions(OrderStatus from, OrderStatus to) {
        Order order = orderIn(from);

        assertThatThrownBy(() -> order.changeStatus(to))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining(from + " -> " + to);
        assertThat(order.getStatus()).isEqualTo(from);
    }

    @Test
    void rejectsNullStatus() {
        assertThatThrownBy(() -> orderIn(OrderStatus.PENDING).changeStatus(null))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void totalIsSumOfItemSubtotalsWithoutPrecisionError() {
        Order order = orderIn(OrderStatus.PENDING);
        order.addItem(OrderItem.custom("A", null, 3, new BigDecimal("0.10")));
        order.addItem(OrderItem.custom("B", null, 1, new BigDecimal("0.20")));
        order.addItem(OrderItem.custom("C", null, 2, new BigDecimal("45.50")));

        assertThat(order.getTotalAmount()).isEqualByComparingTo("91.50");
        assertThat(order.getTotalAmount().scale()).isEqualTo(2);
    }

    @Test
    void statusChangeDoesNotAlterTotal() {
        Order order = orderIn(OrderStatus.PENDING);
        order.addItem(OrderItem.custom("A", null, 2, new BigDecimal("10.00")));

        order.changeStatus(OrderStatus.IN_PRODUCTION);

        assertThat(order.getTotalAmount()).isEqualByComparingTo("20.00");
    }

    @Test
    void rejectsTotalExceedingColumnPrecision() {
        Order order = orderIn(OrderStatus.PENDING);
        order.addItem(OrderItem.custom("A", null, 1, new BigDecimal("9999999999.99")));

        assertThatThrownBy(() -> order.addItem(OrderItem.custom("B", null, 1, BigDecimal.ONE)))
                .isInstanceOf(BusinessRuleException.class);
        assertThat(order.getItems()).hasSize(1);
        assertThat(order.getTotalAmount()).isEqualByComparingTo("9999999999.99");
    }
}
