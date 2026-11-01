package com.doceflux.doceflux_api.dashboard.service;

import com.doceflux.doceflux_api.customer.repository.CustomerRepository;
import com.doceflux.doceflux_api.dashboard.dto.BusinessMetricsDto;
import com.doceflux.doceflux_api.dashboard.dto.DashboardSummaryResponse;
import com.doceflux.doceflux_api.dashboard.dto.FinancialSummaryDto;
import com.doceflux.doceflux_api.dashboard.dto.OrderStatusCountDto;
import com.doceflux.doceflux_api.dashboard.dto.OrdersSummaryDto;
import com.doceflux.doceflux_api.finance.domain.TransactionType;
import com.doceflux.doceflux_api.finance.repository.FinancialTransactionRepository;
import com.doceflux.doceflux_api.order.domain.OrderStatus;
import com.doceflux.doceflux_api.order.repository.OrderRepository;
import com.doceflux.doceflux_api.order.repository.OrderStatusTotals;
import com.doceflux.doceflux_api.product.repository.ProductRepository;
import com.doceflux.doceflux_api.shared.BusinessRuleException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Service
public class DashboardService {

    private static final int MONEY_SCALE = 2;
    private static final ZoneId BUSINESS_ZONE = ZoneId.of("America/Sao_Paulo");

    private final FinancialTransactionRepository transactionRepository;
    private final OrderRepository orderRepository;
    private final CustomerRepository customerRepository;
    private final ProductRepository productRepository;

    public DashboardService(FinancialTransactionRepository transactionRepository,
                            OrderRepository orderRepository,
                            CustomerRepository customerRepository,
                            ProductRepository productRepository) {
        this.transactionRepository = transactionRepository;
        this.orderRepository = orderRepository;
        this.customerRepository = customerRepository;
        this.productRepository = productRepository;
    }

    @Transactional(readOnly = true)
    public DashboardSummaryResponse summary(LocalDate startDate, LocalDate endDate) {
        if (startDate.isAfter(endDate)) {
            throw new BusinessRuleException("A data inicial não pode ser posterior à data final");
        }
        return new DashboardSummaryResponse(startDate, endDate,
                financialSummary(startDate, endDate),
                ordersSummary(startDate, endDate),
                new BusinessMetricsDto(customerRepository.count(), productRepository.countByActiveTrue()));
    }

    private FinancialSummaryDto financialSummary(LocalDate startDate, LocalDate endDate) {
        BigDecimal incomes = totalOf(TransactionType.INCOME, startDate, endDate);
        BigDecimal expenses = totalOf(TransactionType.EXPENSE, startDate, endDate);
        return new FinancialSummaryDto(incomes, expenses, incomes.subtract(expenses),
                transactionRepository.countByTransactionDateBetween(startDate, endDate));
    }

    private BigDecimal totalOf(TransactionType type, LocalDate startDate, LocalDate endDate) {
        BigDecimal total = transactionRepository.sumAmountByTypeAndPeriod(type, startDate, endDate);
        return (total == null ? BigDecimal.ZERO : total).setScale(MONEY_SCALE, RoundingMode.HALF_UP);
    }

    private OrdersSummaryDto ordersSummary(LocalDate startDate, LocalDate endDate) {
        Instant from = startDate.atStartOfDay(BUSINESS_ZONE).toInstant();
        Instant to = endDate.plusDays(1).atStartOfDay(BUSINESS_ZONE).toInstant();

        Map<OrderStatus, OrderStatusTotals> byStatus = new EnumMap<>(OrderStatus.class);
        orderRepository.summarizeByStatus(from, to).forEach(row -> byStatus.put(row.getStatus(), row));

        List<OrderStatusCountDto> distribution = Arrays.stream(OrderStatus.values())
                .map(status -> new OrderStatusCountDto(status, countOf(byStatus.get(status))))
                .toList();

        long totalOrders = distribution.stream().mapToLong(OrderStatusCountDto::count).sum();
        long validOrders = totalOrders - countOf(byStatus.get(OrderStatus.CANCELLED));
        BigDecimal validAmount = byStatus.entrySet().stream()
                .filter(entry -> entry.getKey() != OrderStatus.CANCELLED)
                .map(entry -> entry.getValue().getTotalAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(MONEY_SCALE, RoundingMode.HALF_UP);

        return new OrdersSummaryDto(totalOrders, validOrders, validAmount,
                averageTicket(validAmount, validOrders), distribution);
    }

    private long countOf(OrderStatusTotals totals) {
        return totals == null ? 0 : totals.getCount();
    }

    private BigDecimal averageTicket(BigDecimal amount, long count) {
        if (count == 0) {
            return BigDecimal.ZERO.setScale(MONEY_SCALE);
        }
        return amount.divide(BigDecimal.valueOf(count), MONEY_SCALE, RoundingMode.HALF_UP);
    }
}
