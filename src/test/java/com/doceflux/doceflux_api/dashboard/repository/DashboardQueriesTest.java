package com.doceflux.doceflux_api.dashboard.repository;

import com.doceflux.doceflux_api.customer.domain.Customer;
import com.doceflux.doceflux_api.customer.repository.CustomerRepository;
import com.doceflux.doceflux_api.finance.domain.FinancialTransaction;
import com.doceflux.doceflux_api.finance.repository.FinancialTransactionRepository;
import com.doceflux.doceflux_api.order.domain.Order;
import com.doceflux.doceflux_api.order.domain.OrderItem;
import com.doceflux.doceflux_api.order.domain.OrderStatus;
import com.doceflux.doceflux_api.order.repository.OrderRepository;
import com.doceflux.doceflux_api.order.repository.OrderStatusTotals;
import com.doceflux.doceflux_api.product.domain.Product;
import com.doceflux.doceflux_api.product.repository.ProductRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class DashboardQueriesTest {

    // Período distante para não colidir com dados reais existentes no banco local.
    private static final LocalDate START = LocalDate.of(1997, 3, 1);
    private static final LocalDate END = LocalDate.of(1997, 3, 31);
    private static final ZoneId ZONE = ZoneId.of("America/Sao_Paulo");

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private FinancialTransactionRepository transactionRepository;

    @Autowired
    private EntityManager entityManager;

    private Instant from() {
        return START.atStartOfDay(ZONE).toInstant();
    }

    private Instant to() {
        return END.plusDays(1).atStartOfDay(ZONE).toInstant();
    }

    private void persistOrder(LocalDateTime createdAt, OrderStatus target, String price) {
        Customer customer = customerRepository.save(new Customer("Maria", "11999999999", null, null));
        Order order = new Order(customer, null);
        order.addItem(OrderItem.custom("Bolo", null, 1, new BigDecimal(price)));
        switch (target) {
            case IN_PRODUCTION -> order.changeStatus(OrderStatus.IN_PRODUCTION);
            case COMPLETED -> {
                order.changeStatus(OrderStatus.IN_PRODUCTION);
                order.changeStatus(OrderStatus.COMPLETED);
            }
            case CANCELLED -> order.changeStatus(OrderStatus.CANCELLED);
            case PENDING -> { }
        }
        orderRepository.saveAndFlush(order);
        entityManager.createNativeQuery("update orders set created_at = :createdAt where id = :id")
                .setParameter("createdAt", createdAt.atZone(ZONE).toInstant())
                .setParameter("id", order.getId())
                .executeUpdate();
    }

    private OrderStatusTotals totalsOf(List<OrderStatusTotals> rows, OrderStatus status) {
        return rows.stream().filter(r -> r.getStatus() == status).findFirst().orElse(null);
    }

    @Test
    void groupsOrdersByStatusUsingPersistedTotalAndIncludesCancelled() {
        persistOrder(START.atStartOfDay(), OrderStatus.PENDING, "10.50");
        persistOrder(START.atTime(12, 0), OrderStatus.PENDING, "20.25");
        persistOrder(START.atTime(13, 0), OrderStatus.COMPLETED, "100.00");
        persistOrder(START.atTime(14, 0), OrderStatus.CANCELLED, "80.00");
        entityManager.clear();

        List<OrderStatusTotals> rows = orderRepository.summarizeByStatus(from(), to());

        assertThat(rows).hasSize(3);
        assertThat(totalsOf(rows, OrderStatus.PENDING).getCount()).isEqualTo(2);
        assertThat(totalsOf(rows, OrderStatus.PENDING).getTotalAmount()).isEqualByComparingTo("30.75");
        assertThat(totalsOf(rows, OrderStatus.COMPLETED).getTotalAmount()).isEqualByComparingTo("100.00");
        assertThat(totalsOf(rows, OrderStatus.CANCELLED).getCount()).isEqualTo(1);
        assertThat(totalsOf(rows, OrderStatus.CANCELLED).getTotalAmount()).isEqualByComparingTo("80.00");
        assertThat(totalsOf(rows, OrderStatus.IN_PRODUCTION)).isNull();
    }

    @Test
    void includesFirstInstantAndWholeLastDayAndExcludesOutsideRange() {
        persistOrder(START.atStartOfDay(), OrderStatus.PENDING, "1.00");
        persistOrder(END.atTime(23, 59, 59, 999_000_000), OrderStatus.PENDING, "2.00");
        persistOrder(START.atStartOfDay().minusNanos(1_000), OrderStatus.PENDING, "100.00");
        persistOrder(END.plusDays(1).atStartOfDay(), OrderStatus.PENDING, "200.00");
        entityManager.clear();

        List<OrderStatusTotals> rows = orderRepository.summarizeByStatus(from(), to());

        assertThat(rows).hasSize(1);
        assertThat(rows.getFirst().getCount()).isEqualTo(2);
        assertThat(rows.getFirst().getTotalAmount()).isEqualByComparingTo("3.00");
    }

    @Test
    void returnsNoRowsForPeriodWithoutOrders() {
        assertThat(orderRepository.summarizeByStatus(
                LocalDate.of(1996, 1, 1).atStartOfDay(ZONE).toInstant(),
                LocalDate.of(1996, 2, 1).atStartOfDay(ZONE).toInstant())).isEmpty();
    }

    @Test
    void countsTransactionsOfBothTypesWithinInclusivePeriod() {
        transactionRepository.save(FinancialTransaction.income("Dentro 1", BigDecimal.TEN, START, null, null));
        transactionRepository.save(FinancialTransaction.expense("Dentro 2", BigDecimal.ONE, END, null));
        transactionRepository.save(FinancialTransaction.income("Fora 1", BigDecimal.TEN, START.minusDays(1), null, null));
        transactionRepository.save(FinancialTransaction.expense("Fora 2", BigDecimal.ONE, END.plusDays(1), null));
        transactionRepository.flush();

        assertThat(transactionRepository.countByTransactionDateBetween(START, END)).isEqualTo(2);
    }

    @Test
    void countsOnlyActiveProducts() {
        long before = productRepository.countByActiveTrue();
        Product active = productRepository.save(new Product("Ativo", null, BigDecimal.TEN));
        Product inactive = productRepository.save(new Product("Inativo", null, BigDecimal.TEN));
        entityManager.flush();
        entityManager.createNativeQuery("update products set active = false where id = :id")
                .setParameter("id", inactive.getId()).executeUpdate();
        entityManager.clear();

        assertThat(active.isActive()).isTrue();
        assertThat(productRepository.countByActiveTrue()).isEqualTo(before + 1);
    }
}
