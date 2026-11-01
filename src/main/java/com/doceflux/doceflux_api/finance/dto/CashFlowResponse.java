package com.doceflux.doceflux_api.finance.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CashFlowResponse(
        LocalDate startDate,
        LocalDate endDate,
        BigDecimal totalIncome,
        BigDecimal totalExpense,
        BigDecimal balance) {
}
