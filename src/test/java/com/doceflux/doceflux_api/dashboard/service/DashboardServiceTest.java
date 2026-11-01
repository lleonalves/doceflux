package com.doceflux.doceflux_api.dashboard.service;

import com.doceflux.doceflux_api.customer.repository.CustomerRepository;
import com.doceflux.doceflux_api.dashboard.dto.DashboardSummaryResponse;
import com.doceflux.doceflux_api.dashboard.dto.OrderStatusCountDto;
import com.doceflux.doceflux_api.finance.domain.TransactionType;
import com.doceflux.doceflux_api.finance.repository.FinancialTransactionRepository;
import com.doceflux.doceflux_api.order.domain.OrderStatus;
import com.doceflux.doceflux_api.order.repository.OrderRepository;
import com.doceflux.doceflux_api.order.repository.OrderStatusTotals;
import com.doceflux.doceflux_api.product.repository.ProductRepository;
import com.doceflux.doceflux_api.shared.BusinessRuleException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {

    private static final LocalDate START = LocalDate.of(2026, 10, 1);
    private static final LocalDate END = LocalDate.of(2026, 10, 31);

    @Mock
    private FinancialTransactionRepository transactionRepository;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private DashboardService dashboardService;

    private record Row(OrderStatus getStatus, long getCount, BigDecimal getTotalAmount) implements OrderStatusTotals {
    }

    private Row row(OrderStatus status, long count, String amount) {
        return new Row(status, count, new BigDecimal(amount));
    }

    private void mockFinance(String incomes, String expenses, long count) {
        when(transactionRepository.sumAmountByTypeAndPeriod(TransactionType.INCOME, START, END))
                .thenReturn(incomes == null ? null : new BigDecimal(incomes));
        when(transactionRepository.sumAmountByTypeAndPeriod(TransactionType.EXPENSE, START, END))
                .thenReturn(expenses == null ? null : new BigDecimal(expenses));
        when(transactionRepository.countByTransactionDateBetween(START, END)).thenReturn(count);
    }

    private void mockOrders(Row... rows) {
        when(orderRepository.summarizeByStatus(any(), any())).thenReturn(List.of(rows));
    }

    private DashboardSummaryResponse summaryWithOrders(Row... rows) {
        mockFinance(null, null, 0);
        mockOrders(rows);
        return dashboardService.summary(START, END);
    }

    @Test
    void consolidatesFinancialSummaryWithPositiveBalance() {
        mockFinance("5000.00", "1800.00", 35);
        mockOrders();

        var financial = dashboardService.summary(START, END).financialSummary();

        assertThat(financial.totalIncomes()).isEqualByComparingTo("5000.00");
        assertThat(financial.totalExpenses()).isEqualByComparingTo("1800.00");
        assertThat(financial.netBalance()).isEqualByComparingTo("3200.00");
        assertThat(financial.transactionsCount()).isEqualTo(35);
    }

    @Test
    void allowsNegativeBalance() {
        mockFinance("100.00", "250.50", 2);
        mockOrders();

        assertThat(dashboardService.summary(START, END).financialSummary().netBalance())
                .isEqualByComparingTo("-150.50");
    }

    @Test
    void returnsZeroWhenThereAreNoIncomes() {
        mockFinance(null, "40.00", 1);
        mockOrders();

        var financial = dashboardService.summary(START, END).financialSummary();

        assertThat(financial.totalIncomes()).isEqualByComparingTo("0.00");
        assertThat(financial.netBalance()).isEqualByComparingTo("-40.00");
    }

    @Test
    void returnsZeroWhenThereAreNoExpenses() {
        mockFinance("40.00", null, 1);
        mockOrders();

        var financial = dashboardService.summary(START, END).financialSummary();

        assertThat(financial.totalExpenses()).isEqualByComparingTo("0.00");
        assertThat(financial.netBalance()).isEqualByComparingTo("40.00");
    }

    @Test
    void returnsZeroesWithoutAnyTransaction() {
        mockFinance(null, null, 0);
        mockOrders();

        var financial = dashboardService.summary(START, END).financialSummary();

        assertThat(financial.totalIncomes()).isEqualByComparingTo("0");
        assertThat(financial.totalExpenses()).isEqualByComparingTo("0");
        assertThat(financial.netBalance()).isEqualByComparingTo("0");
        assertThat(financial.transactionsCount()).isZero();
    }

    @Test
    void ordersAreNeverUsedAsIncome() {
        var response = summaryWithOrders(row(OrderStatus.COMPLETED, 3, "900.00"));

        assertThat(response.financialSummary().totalIncomes()).isEqualByComparingTo("0");
        assertThat(response.ordersSummary().totalOrdersAmount()).isEqualByComparingTo("900.00");
    }

    @Test
    void excludesCancelledOrdersFromAmountAndAverageTicketButKeepsThemInTotal() {
        var orders = summaryWithOrders(
                row(OrderStatus.COMPLETED, 1, "100.00"),
                row(OrderStatus.PENDING, 1, "50.00"),
                row(OrderStatus.IN_PRODUCTION, 1, "150.00"),
                row(OrderStatus.CANCELLED, 1, "80.00")).ordersSummary();

        assertThat(orders.totalOrders()).isEqualTo(4);
        assertThat(orders.validOrdersCount()).isEqualTo(3);
        assertThat(orders.totalOrdersAmount()).isEqualByComparingTo("300.00");
        assertThat(orders.averageTicket()).isEqualByComparingTo("100.00");
    }

    @Test
    void roundsAverageTicketHalfUpToTwoDecimals() {
        var orders = summaryWithOrders(row(OrderStatus.PENDING, 3, "100.00")).ordersSummary();
        assertThat(orders.averageTicket()).isEqualByComparingTo("33.33");
        assertThat(orders.averageTicket().scale()).isEqualTo(2);

        orders = summaryWithOrders(row(OrderStatus.COMPLETED, 2, "0.01")).ordersSummary();
        assertThat(orders.averageTicket()).isEqualByComparingTo("0.01");
    }

    @Test
    void averageTicketIsZeroWithoutValidOrders() {
        assertThat(summaryWithOrders().ordersSummary().averageTicket()).isEqualByComparingTo("0.00");

        var onlyCancelled = summaryWithOrders(row(OrderStatus.CANCELLED, 2, "200.00")).ordersSummary();
        assertThat(onlyCancelled.totalOrders()).isEqualTo(2);
        assertThat(onlyCancelled.validOrdersCount()).isZero();
        assertThat(onlyCancelled.totalOrdersAmount()).isEqualByComparingTo("0.00");
        assertThat(onlyCancelled.averageTicket()).isEqualByComparingTo("0.00");
    }

    @Test
    void countsAllNonCancelledStatusesAsValid() {
        var orders = summaryWithOrders(
                row(OrderStatus.PENDING, 2, "20.00"),
                row(OrderStatus.IN_PRODUCTION, 3, "30.00"),
                row(OrderStatus.COMPLETED, 4, "40.00")).ordersSummary();

        assertThat(orders.validOrdersCount()).isEqualTo(9);
        assertThat(orders.totalOrdersAmount()).isEqualByComparingTo("90.00");
    }

    @Test
    void distributionContainsEveryStatusIncludingZeroAndCancelled() {
        var distribution = summaryWithOrders(
                row(OrderStatus.COMPLETED, 30, "1.00"),
                row(OrderStatus.CANCELLED, 2, "1.00")).ordersSummary().ordersByStatus();

        assertThat(distribution).containsExactly(
                new OrderStatusCountDto(OrderStatus.PENDING, 0),
                new OrderStatusCountDto(OrderStatus.IN_PRODUCTION, 0),
                new OrderStatusCountDto(OrderStatus.COMPLETED, 30),
                new OrderStatusCountDto(OrderStatus.CANCELLED, 2));
    }

    @Test
    void distributionHasAllStatusesWhenThereAreNoOrders() {
        var distribution = summaryWithOrders().ordersSummary().ordersByStatus();

        assertThat(distribution).extracting(OrderStatusCountDto::status).containsExactly(OrderStatus.values());
        assertThat(distribution).extracting(OrderStatusCountDto::count).containsOnly(0L);
    }

    @Test
    void queriesOrdersWithHalfOpenRangeCoveringWholeLastDay() {
        summaryWithOrders();

        ArgumentCaptor<Instant> from = ArgumentCaptor.forClass(Instant.class);
        ArgumentCaptor<Instant> to = ArgumentCaptor.forClass(Instant.class);
        verify(orderRepository).summarizeByStatus(from.capture(), to.capture());
        ZoneId zone = ZoneId.of("America/Sao_Paulo");
        assertThat(from.getValue()).isEqualTo(START.atStartOfDay(zone).toInstant());
        assertThat(to.getValue()).isEqualTo(END.plusDays(1).atStartOfDay(zone).toInstant());
    }

    @Test
    void returnsCustomerAndActiveProductCounts() {
        mockFinance(null, null, 0);
        mockOrders();
        when(customerRepository.count()).thenReturn(120L);
        when(productRepository.countByActiveTrue()).thenReturn(35L);

        var metrics = dashboardService.summary(START, END).businessMetrics();

        assertThat(metrics.totalCustomers()).isEqualTo(120);
        assertThat(metrics.activeProductsCount()).isEqualTo(35);
    }

    @Test
    void echoesRequestedPeriod() {
        var response = summaryWithOrders();

        assertThat(response.startDate()).isEqualTo(START);
        assertThat(response.endDate()).isEqualTo(END);
    }

    @Test
    void acceptsSingleDayPeriod() {
        when(transactionRepository.sumAmountByTypeAndPeriod(any(), any(), any())).thenReturn(null);
        mockOrders();

        assertThat(dashboardService.summary(START, START).startDate()).isEqualTo(START);
    }

    @Test
    void rejectsStartDateAfterEndDate() {
        assertThatThrownBy(() -> dashboardService.summary(END, START))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("A data inicial não pode ser posterior à data final");

        verifyNoInteractions(transactionRepository, orderRepository, customerRepository, productRepository);
        verify(orderRepository, never()).summarizeByStatus(any(), any());
    }
}
