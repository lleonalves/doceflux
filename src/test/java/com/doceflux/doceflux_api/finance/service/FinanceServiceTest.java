package com.doceflux.doceflux_api.finance.service;

import com.doceflux.doceflux_api.finance.domain.FinancialTransaction;
import com.doceflux.doceflux_api.finance.domain.TransactionType;
import com.doceflux.doceflux_api.finance.dto.CashFlowResponse;
import com.doceflux.doceflux_api.finance.dto.ExpenseRequest;
import com.doceflux.doceflux_api.finance.dto.IncomeRequest;
import com.doceflux.doceflux_api.finance.repository.FinancialTransactionRepository;
import com.doceflux.doceflux_api.order.repository.OrderRepository;
import com.doceflux.doceflux_api.shared.BusinessRuleException;
import com.doceflux.doceflux_api.shared.ResourceNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FinanceServiceTest {

    private static final LocalDate START = LocalDate.of(2026, 10, 1);
    private static final LocalDate END = LocalDate.of(2026, 10, 31);

    @Mock
    private FinancialTransactionRepository transactionRepository;

    @Mock
    private OrderRepository orderRepository;

    @InjectMocks
    private FinanceService financeService;

    private void mockSave() {
        when(transactionRepository.save(any(FinancialTransaction.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void registersIncomeWithoutOrder() {
        mockSave();

        FinancialTransaction income = financeService.registerIncome(
                new IncomeRequest(" Venda balcão ", new BigDecimal("50.5"), START, "  Vendas ", null));

        assertThat(income.getId()).isNotNull();
        assertThat(income.getType()).isEqualTo(TransactionType.INCOME);
        assertThat(income.getDescription()).isEqualTo("Venda balcão");
        assertThat(income.getAmount()).isEqualByComparingTo("50.50");
        assertThat(income.getAmount().scale()).isEqualTo(2);
        assertThat(income.getTransactionDate()).isEqualTo(START);
        assertThat(income.getCategory()).isEqualTo("Vendas");
        assertThat(income.getOrderId()).isNull();
        assertThat(income.getCreatedAt()).isNotNull();
        verifyNoInteractions(orderRepository);
    }

    @Test
    void registersIncomeLinkedToExistingOrderAndCreatesOnlyOneTransaction() {
        UUID orderId = UUID.randomUUID();
        when(orderRepository.existsById(orderId)).thenReturn(true);
        mockSave();

        FinancialTransaction income = financeService.registerIncome(
                new IncomeRequest("Sinal", new BigDecimal("30.00"), START, null, orderId));

        assertThat(income.getOrderId()).isEqualTo(orderId);
        verify(transactionRepository, times(1)).save(any());
        verify(orderRepository).existsById(orderId);
        verify(orderRepository, never()).save(any());
    }

    @Test
    void rejectsIncomeLinkedToUnknownOrder() {
        UUID orderId = UUID.randomUUID();
        when(orderRepository.existsById(orderId)).thenReturn(false);

        assertThatThrownBy(() -> financeService.registerIncome(
                new IncomeRequest("Sinal", BigDecimal.TEN, START, null, orderId)))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(transactionRepository, never()).save(any());
    }

    @Test
    void registersExpenseWithoutOrder() {
        mockSave();

        FinancialTransaction expense = financeService.registerExpense(
                new ExpenseRequest("Farinha", new BigDecimal("120"), START, "Insumos"));

        assertThat(expense.getType()).isEqualTo(TransactionType.EXPENSE);
        assertThat(expense.getAmount()).isEqualByComparingTo("120.00");
        assertThat(expense.getCategory()).isEqualTo("Insumos");
        assertThat(expense.getOrderId()).isNull();
        verifyNoInteractions(orderRepository);
    }

    @Test
    void throwsWhenTransactionDoesNotExist() {
        UUID id = UUID.randomUUID();
        when(transactionRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> financeService.findById(id)).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void calculatesCashFlowBalanceWithBigDecimal() {
        when(transactionRepository.sumAmountByTypeAndPeriod(TransactionType.INCOME, START, END))
                .thenReturn(new BigDecimal("300.10"));
        when(transactionRepository.sumAmountByTypeAndPeriod(TransactionType.EXPENSE, START, END))
                .thenReturn(new BigDecimal("100.20"));

        CashFlowResponse cashFlow = financeService.cashFlow(START, END);

        assertThat(cashFlow.startDate()).isEqualTo(START);
        assertThat(cashFlow.endDate()).isEqualTo(END);
        assertThat(cashFlow.totalIncome()).isEqualByComparingTo("300.10");
        assertThat(cashFlow.totalExpense()).isEqualByComparingTo("100.20");
        assertThat(cashFlow.balance()).isEqualByComparingTo("199.90");
    }

    @Test
    void returnsZeroedCashFlowForPeriodWithoutTransactions() {
        when(transactionRepository.sumAmountByTypeAndPeriod(any(), any(), any())).thenReturn(null);

        CashFlowResponse cashFlow = financeService.cashFlow(START, END);

        assertThat(cashFlow.totalIncome()).isEqualByComparingTo("0.00");
        assertThat(cashFlow.totalExpense()).isEqualByComparingTo("0.00");
        assertThat(cashFlow.balance()).isEqualByComparingTo("0.00");
    }

    @Test
    void reportsNegativeBalanceWhenExpensesExceedIncome() {
        when(transactionRepository.sumAmountByTypeAndPeriod(TransactionType.INCOME, START, END))
                .thenReturn(new BigDecimal("10.00"));
        when(transactionRepository.sumAmountByTypeAndPeriod(TransactionType.EXPENSE, START, END))
                .thenReturn(new BigDecimal("25.50"));

        assertThat(financeService.cashFlow(START, END).balance()).isEqualByComparingTo("-15.50");
    }

    @Test
    void rejectsPeriodWithStartAfterEnd() {
        assertThatThrownBy(() -> financeService.cashFlow(END, START)).isInstanceOf(BusinessRuleException.class);

        verifyNoInteractions(transactionRepository);
    }
}
