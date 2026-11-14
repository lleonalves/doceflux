package com.doceflux.doceflux_api.finance.repository;

import com.doceflux.doceflux_api.customer.domain.Customer;
import com.doceflux.doceflux_api.customer.repository.CustomerRepository;
import com.doceflux.doceflux_api.finance.domain.FinancialTransaction;
import com.doceflux.doceflux_api.finance.domain.TransactionType;
import com.doceflux.doceflux_api.order.domain.Order;
import com.doceflux.doceflux_api.order.repository.OrderRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class FinancialTransactionRepositoryTest {

    private static final UUID STORE = UUID.randomUUID();

    // Período distante para não colidir com dados reais existentes no banco local.
    private static final LocalDate START = LocalDate.of(1999, 1, 1);
    private static final LocalDate END = LocalDate.of(1999, 1, 31);

    @Autowired
    private FinancialTransactionRepository transactionRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private EntityManager entityManager;

    private Order persistOrder() {
        Customer customer = customerRepository.save(new Customer(STORE, "Maria", "11999999999", null, null));
        return orderRepository.saveAndFlush(new Order(STORE, customer, "Pedido"));
    }

    @Test
    void persistsIncomeAndExpenseWithAllFields() {
        FinancialTransaction income = transactionRepository.save(FinancialTransaction.income(STORE, 
                "Venda", new BigDecimal("50.50"), START, "Vendas", null));
        FinancialTransaction expense = transactionRepository.save(FinancialTransaction.expense(STORE, 
                "Farinha", new BigDecimal("20.00"), START, null));
        transactionRepository.flush();
        entityManager.clear();

        FinancialTransaction foundIncome = transactionRepository.findByIdAndStoreId(income.getId(), STORE).orElseThrow();
        FinancialTransaction foundExpense = transactionRepository.findByIdAndStoreId(expense.getId(), STORE).orElseThrow();

        assertThat(foundIncome.getType()).isEqualTo(TransactionType.INCOME);
        assertThat(foundIncome.getAmount()).isEqualByComparingTo("50.50");
        assertThat(foundIncome.getTransactionDate()).isEqualTo(START);
        assertThat(foundIncome.getCategory()).isEqualTo("Vendas");
        assertThat(foundIncome.getOrderId()).isNull();
        assertThat(foundIncome.getCreatedAt()).isNotNull();
        assertThat(foundIncome.getUpdatedAt()).isNotNull();
        assertThat(foundExpense.getType()).isEqualTo(TransactionType.EXPENSE);
        assertThat(foundExpense.getCategory()).isNull();
    }

    @Test
    void allowsMultiplePartialIncomesForTheSameOrderWithoutChangingIt() {
        Order order = persistOrder();
        entityManager.clear();
        var updatedAtBefore = orderRepository.findByIdAndStoreId(order.getId(), STORE).orElseThrow().getUpdatedAt();
        entityManager.clear();

        transactionRepository.save(FinancialTransaction.income(STORE, "Sinal", new BigDecimal("30.00"), START, null, order.getId()));
        transactionRepository.save(FinancialTransaction.income(STORE, "Restante", new BigDecimal("70.00"), END, null, order.getId()));
        transactionRepository.flush();
        entityManager.clear();

        List<FinancialTransaction> linked = transactionRepository.findAll().stream()
                .filter(t -> order.getId().equals(t.getOrderId())).toList();
        assertThat(linked).hasSize(2);
        assertThat(linked).extracting(FinancialTransaction::getId).doesNotHaveDuplicates();
        assertThat(linked).extracting(FinancialTransaction::getAmount)
                .usingComparatorForType(BigDecimal::compareTo, BigDecimal.class)
                .containsExactlyInAnyOrder(new BigDecimal("30.00"), new BigDecimal("70.00"));

        Order reloaded = orderRepository.findByIdAndStoreId(order.getId(), STORE).orElseThrow();
        assertThat(reloaded.getUpdatedAt()).isEqualTo(updatedAtBefore);
        assertThat(reloaded.getItems()).isEmpty();
    }

    @Test
    void sumsByTypeOnlyWithinThePeriod() {
        transactionRepository.save(FinancialTransaction.income(STORE, "In 1", new BigDecimal("100.10"), START, null, null));
        transactionRepository.save(FinancialTransaction.income(STORE, "In 2", new BigDecimal("200.20"), END, null, null));
        transactionRepository.save(FinancialTransaction.income(STORE, "Fora", new BigDecimal("999.00"), END.plusDays(1), null, null));
        transactionRepository.save(FinancialTransaction.expense(STORE, "Out 1", new BigDecimal("50.05"), START, null));
        transactionRepository.save(FinancialTransaction.expense(STORE, "Fora", new BigDecimal("999.00"), START.minusDays(1), null));
        transactionRepository.flush();

        BigDecimal income = transactionRepository.sumAmountByStoreIdAndTypeAndPeriod(STORE, TransactionType.INCOME, START, END);
        BigDecimal expense = transactionRepository.sumAmountByStoreIdAndTypeAndPeriod(STORE, TransactionType.EXPENSE, START, END);

        assertThat(income).isEqualByComparingTo("300.30");
        assertThat(expense).isEqualByComparingTo("50.05");
    }

    @Test
    void returnsNullSumForPeriodWithoutTransactions() {
        assertThat(transactionRepository.sumAmountByStoreIdAndTypeAndPeriod(STORE,
                TransactionType.INCOME, LocalDate.of(1998, 1, 1), LocalDate.of(1998, 1, 31))).isNull();
    }

    private static final LocalDate P_START = LocalDate.of(1997, 3, 10);
    private static final LocalDate P_END = LocalDate.of(1997, 3, 20);

    private List<UUID> idsOf(TransactionType type, UUID orderId, LocalDate start, LocalDate end) {
        return transactionRepository
                .findAll(FinancialTransactionSpecifications.withFilters(STORE, type, orderId, start, end))
                .stream().map(FinancialTransaction::getId).toList();
    }

    @Test
    void filtersByTypeOrderAndPeriodWithInclusiveBoundaries() {
        Order order = persistOrder();
        UUID otherOrderId = persistOrder().getId();
        FinancialTransaction before = transactionRepository.save(
                FinancialTransaction.expense(STORE, "Antes", BigDecimal.ONE, P_START.minusDays(1), null));
        FinancialTransaction atStart = transactionRepository.save(
                FinancialTransaction.income(STORE, "Início", BigDecimal.ONE, P_START, null, order.getId()));
        FinancialTransaction inside = transactionRepository.save(
                FinancialTransaction.expense(STORE, "Dentro", BigDecimal.ONE, P_START.plusDays(5), null));
        FinancialTransaction atEnd = transactionRepository.save(
                FinancialTransaction.income(STORE, "Fim", BigDecimal.ONE, P_END, null, order.getId()));
        FinancialTransaction after = transactionRepository.save(
                FinancialTransaction.income(STORE, "Depois", BigDecimal.ONE, P_END.plusDays(1), null, otherOrderId));
        transactionRepository.flush();
        entityManager.clear();

        assertThat(idsOf(null, null, P_START, P_END))
                .containsExactlyInAnyOrder(atStart.getId(), inside.getId(), atEnd.getId());
        assertThat(idsOf(null, null, P_START, null))
                .contains(atStart.getId(), inside.getId(), atEnd.getId(), after.getId())
                .doesNotContain(before.getId());
        assertThat(idsOf(null, null, null, P_END))
                .contains(before.getId(), atStart.getId(), inside.getId(), atEnd.getId())
                .doesNotContain(after.getId());
        assertThat(idsOf(TransactionType.EXPENSE, null, P_START, P_END)).containsExactly(inside.getId());
        assertThat(idsOf(TransactionType.INCOME, null, P_START, P_END))
                .containsExactlyInAnyOrder(atStart.getId(), atEnd.getId());
        assertThat(idsOf(null, order.getId(), null, null))
                .containsExactlyInAnyOrder(atStart.getId(), atEnd.getId());
        assertThat(idsOf(TransactionType.INCOME, order.getId(), P_START.plusDays(1), P_END))
                .containsExactly(atEnd.getId());
        assertThat(idsOf(TransactionType.EXPENSE, order.getId(), null, null)).isEmpty();
    }

    @Test
    void deletedTransactionIsNotListedNorSummedAndOrderIsKept() {
        Order order = persistOrder();
        FinancialTransaction kept = transactionRepository.save(
                FinancialTransaction.income(STORE, "Fica", new BigDecimal("10.00"), P_START, null, order.getId()));
        FinancialTransaction removed = transactionRepository.save(
                FinancialTransaction.income(STORE, "Sai", new BigDecimal("5.00"), P_START, null, order.getId()));
        transactionRepository.flush();
        entityManager.clear();

        transactionRepository.deleteById(removed.getId());
        transactionRepository.flush();
        entityManager.clear();

        assertThat(idsOf(null, order.getId(), null, null)).containsExactly(kept.getId());
        assertThat(transactionRepository.existsById(removed.getId())).isFalse();
        assertThat(transactionRepository.sumAmountByStoreIdAndTypeAndPeriod(STORE, TransactionType.INCOME, P_START, P_START))
                .isEqualByComparingTo("10.00");
        assertThat(orderRepository.existsById(order.getId())).isTrue();
    }

    @Test
    void databaseRejectsIncomeLinkedToUnknownOrder() {
        assertThatThrownBy(() -> transactionRepository.saveAndFlush(FinancialTransaction.income(STORE, 
                "Sinal", BigDecimal.TEN, START, null, UUID.randomUUID())))
                .isInstanceOf(Exception.class);
    }

    @Test
    void databaseRejectsExpenseLinkedToOrder() {
        Order order = persistOrder();

        assertThatThrownBy(() -> entityManager.createNativeQuery("""
                insert into financial_transactions (id, type, description, amount, transaction_date, order_id,
                                                    created_at, updated_at)
                values (:id, 'EXPENSE', 'x', 10.00, current_date, :orderId, now(), now())
                """)
                .setParameter("id", UUID.randomUUID())
                .setParameter("orderId", order.getId())
                .executeUpdate())
                .isInstanceOf(Exception.class);
    }
}
