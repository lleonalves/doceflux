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
import com.doceflux.doceflux_api.finance.repository.FinancialTransactionSpecifications;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
public class FinanceService {

    private static final int MONEY_SCALE = 2;

    private final FinancialTransactionRepository transactionRepository;
    private final OrderRepository orderRepository;

    public FinanceService(FinancialTransactionRepository transactionRepository, OrderRepository orderRepository) {
        this.transactionRepository = transactionRepository;
        this.orderRepository = orderRepository;
    }

    @Transactional
    public FinancialTransaction registerIncome(IncomeRequest request) {
        if (request.orderId() != null && !orderRepository.existsById(request.orderId())) {
            throw new ResourceNotFoundException("Pedido não encontrado: " + request.orderId());
        }
        return transactionRepository.save(FinancialTransaction.income(
                request.description().trim(), request.amount(), request.date(),
                normalizeCategory(request.category()), request.orderId()));
    }

    @Transactional
    public FinancialTransaction registerExpense(ExpenseRequest request) {
        return transactionRepository.save(FinancialTransaction.expense(
                request.description().trim(), request.amount(), request.date(),
                normalizeCategory(request.category())));
    }

    @Transactional(readOnly = true)
    public List<FinancialTransaction> findAll(TransactionType type, UUID orderId,
                                              LocalDate startDate, LocalDate endDate) {
        if (startDate != null && endDate != null && startDate.isAfter(endDate)) {
            throw new BusinessRuleException("A data inicial não pode ser posterior à data final");
        }
        return transactionRepository.findAll(
                FinancialTransactionSpecifications.withFilters(type, orderId, startDate, endDate),
                Sort.by(Sort.Order.desc("transactionDate"), Sort.Order.desc("createdAt")));
    }

    @Transactional
    public void delete(UUID id) {
        transactionRepository.delete(findById(id));
    }

    @Transactional(readOnly = true)
    public FinancialTransaction findById(UUID id) {
        return transactionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Movimentação não encontrada: " + id));
    }

    @Transactional(readOnly = true)
    public CashFlowResponse cashFlow(LocalDate startDate, LocalDate endDate) {
        if (startDate.isAfter(endDate)) {
            throw new BusinessRuleException("A data inicial não pode ser posterior à data final");
        }
        BigDecimal totalIncome = totalOf(TransactionType.INCOME, startDate, endDate);
        BigDecimal totalExpense = totalOf(TransactionType.EXPENSE, startDate, endDate);
        return new CashFlowResponse(startDate, endDate, totalIncome, totalExpense,
                totalIncome.subtract(totalExpense));
    }

    private BigDecimal totalOf(TransactionType type, LocalDate startDate, LocalDate endDate) {
        BigDecimal total = transactionRepository.sumAmountByTypeAndPeriod(type, startDate, endDate);
        return (total == null ? BigDecimal.ZERO : total).setScale(MONEY_SCALE);
    }

    private String normalizeCategory(String category) {
        return category == null || category.isBlank() ? null : category.trim();
    }
}
