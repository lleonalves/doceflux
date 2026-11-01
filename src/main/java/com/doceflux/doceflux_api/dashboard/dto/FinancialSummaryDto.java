package com.doceflux.doceflux_api.dashboard.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

public record FinancialSummaryDto(
        @Schema(description = "Soma das receitas registradas no período", example = "5000.00")
        BigDecimal totalIncomes,
        @Schema(description = "Soma das despesas registradas no período", example = "1800.00")
        BigDecimal totalExpenses,
        @Schema(description = "Receitas menos despesas; pode ser negativo", example = "3200.00")
        BigDecimal netBalance,
        @Schema(description = "Quantidade de transações financeiras no período", example = "35")
        long transactionsCount) {
}
