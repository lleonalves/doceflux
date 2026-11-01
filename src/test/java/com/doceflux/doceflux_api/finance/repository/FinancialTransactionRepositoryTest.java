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
        Customer customer = customerRepository.save(new Customer("Maria", "11999999999", null, null));
        return orderRepository.saveAndFlush(new Order(customer, "Pedido"));
    }

    @Test
    void persistsIncomeAndExpenseWithAllFields() {
        FinancialTransaction income = transactionRepository.save(FinancialTransaction.income(
                "Venda", new BigDecimal("50.50"), START, "Vendas", null));
        FinancialTransaction expense = transactionRepository.save(FinancialTransaction.expense(
                "Farinha", new BigDecimal("20.00"), START, null));
        transactionRepository.flush();
        entityManager.clear();

        FinancialTransaction foundIncome = transactionRepository.findById(income.getId()).orElseThrow();
        FinancialTransaction foundExpense = transactionRepository.findById(expense.getId()).orElseThrow();

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
        var updatedAtBefore = orderRepository.findById(order.getId()).orElseThrow().getUpdatedAt();
        entityManager.clear();

        transactionRepository.save(FinancialTransaction.income("Sinal", new BigDecimal("30.00"), START, null, order.getId()));
        transactionRepository.save(FinancialTransaction.income("Restante", new BigDecimal("70.00"), END, null, order.getId()));
        transactionRepository.flush();
        entityManager.clear();

        List<FinancialTransaction> linked = transactionRepository.findAll().stream()
                .filter(t -> order.getId().equals(t.getOrderId())).toList();
        assertThat(linked).hasSize(2);
        assertThat(linked).extracting(FinancialTransaction::getId).doesNotHaveDuplicates();
        assertThat(linked).extracting(FinancialTransaction::getAmount)
                .usingComparatorForType(BigDecimal::compareTo, BigDecimal.class)
                .containsExactlyInAnyOrder(new BigDecimal("30.00"), new BigDecimal("70.00"));

        Order reloaded = orderRepository.findById(order.getId()).orElseThrow();
        assertThat(reloaded.getUpdatedAt()).isEqualTo(updatedAtBefore);
        assertThat(reloaded.getItems()).isEmpty();
    }

    @Test
    void sumsByTypeOnlyWithinThePeriod() {
        transactionRepository.save(FinancialTransaction.income("In 1", new BigDecimal("100.10"), START, null, null));
        transactionRepository.save(FinancialTransaction.income("In 2", new BigDecimal("200.20"), END, null, null));
        transactionRepository.save(FinancialTransaction.income("Fora", new BigDecimal("999.00"), END.plusDays(1), null, null));
        transactionRepository.save(FinancialTransaction.expense("Out 1", new BigDecimal("50.05"), START, null));
        transactionRepository.save(FinancialTransaction.expense("Fora", new BigDecimal("999.00"), START.minusDays(1), null));
        transactionRepository.flush();

        BigDecimal income = transactionRepository.sumAmountByTypeAndPeriod(TransactionType.INCOME, START, END);
        BigDecimal expense = transactionRepository.sumAmountByTypeAndPeriod(TransactionType.EXPENSE, START, END);

        assertThat(income).isEqualByComparingTo("300.30");
        assertThat(expense).isEqualByComparingTo("50.05");
    }

    @Test
    void returnsNullSumForPeriodWithoutTransactions() {
        assertThat(transactionRepository.sumAmountByTypeAndPeriod(
                TransactionType.INCOME, LocalDate.of(1998, 1, 1), LocalDate.of(1998, 1, 31))).isNull();
    }

    @Test
    void databaseRejectsIncomeLinkedToUnknownOrder() {
        assertThatThrownBy(() -> transactionRepository.saveAndFlush(FinancialTransaction.income(
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
