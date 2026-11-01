package com.doceflux.doceflux_api.dashboard.dto;

import java.time.LocalDate;

public record DashboardSummaryResponse(
        LocalDate startDate,
        LocalDate endDate,
        FinancialSummaryDto financialSummary,
        OrdersSummaryDto ordersSummary,
        BusinessMetricsDto businessMetrics) {
}
