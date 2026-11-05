package com.doceflux.doceflux_api.dashboard.controller;

import com.doceflux.doceflux_api.dashboard.dto.BusinessMetricsDto;
import com.doceflux.doceflux_api.dashboard.dto.DashboardSummaryResponse;
import com.doceflux.doceflux_api.dashboard.dto.FinancialSummaryDto;
import com.doceflux.doceflux_api.dashboard.dto.OrderStatusCountDto;
import com.doceflux.doceflux_api.dashboard.dto.OrdersSummaryDto;
import com.doceflux.doceflux_api.dashboard.service.DashboardService;
import com.doceflux.doceflux_api.order.domain.OrderStatus;
import com.doceflux.doceflux_api.shared.BusinessRuleException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(DashboardController.class)
@AutoConfigureMockMvc(addFilters = false)
class DashboardControllerTest {

    private static final LocalDate START = LocalDate.of(2026, 10, 1);
    private static final LocalDate END = LocalDate.of(2026, 10, 31);

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DashboardService dashboardService;

    private DashboardSummaryResponse sample() {
        return new DashboardSummaryResponse(START, END,
                new FinancialSummaryDto(new BigDecimal("5000.00"), new BigDecimal("1800.00"),
                        new BigDecimal("3200.00"), 35),
                new OrdersSummaryDto(42, 40, new BigDecimal("6000.00"), new BigDecimal("150.00"), List.of(
                        new OrderStatusCountDto(OrderStatus.PENDING, 5),
                        new OrderStatusCountDto(OrderStatus.IN_PRODUCTION, 5),
                        new OrderStatusCountDto(OrderStatus.COMPLETED, 30),
                        new OrderStatusCountDto(OrderStatus.CANCELLED, 2))),
                new BusinessMetricsDto(120, 35));
    }

    @Test
    void returnsSummary() throws Exception {
        when(dashboardService.summary(START, END)).thenReturn(sample());

        mockMvc.perform(get("/api/dashboard/summary").param("startDate", "2026-10-01").param("endDate", "2026-10-31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.startDate").value("2026-10-01"))
                .andExpect(jsonPath("$.endDate").value("2026-10-31"))
                .andExpect(jsonPath("$.financialSummary.totalIncomes").value(5000.00))
                .andExpect(jsonPath("$.financialSummary.totalExpenses").value(1800.00))
                .andExpect(jsonPath("$.financialSummary.netBalance").value(3200.00))
                .andExpect(jsonPath("$.financialSummary.transactionsCount").value(35))
                .andExpect(jsonPath("$.ordersSummary.totalOrders").value(42))
                .andExpect(jsonPath("$.ordersSummary.validOrdersCount").value(40))
                .andExpect(jsonPath("$.ordersSummary.totalOrdersAmount").value(6000.00))
                .andExpect(jsonPath("$.ordersSummary.averageTicket").value(150.00))
                .andExpect(jsonPath("$.ordersSummary.ordersByStatus.length()").value(4))
                .andExpect(jsonPath("$.ordersSummary.ordersByStatus[0].status").value("PENDING"))
                .andExpect(jsonPath("$.ordersSummary.ordersByStatus[1].status").value("IN_PRODUCTION"))
                .andExpect(jsonPath("$.ordersSummary.ordersByStatus[2].status").value("COMPLETED"))
                .andExpect(jsonPath("$.ordersSummary.ordersByStatus[2].count").value(30))
                .andExpect(jsonPath("$.ordersSummary.ordersByStatus[3].status").value("CANCELLED"))
                .andExpect(jsonPath("$.businessMetrics.totalCustomers").value(120))
                .andExpect(jsonPath("$.businessMetrics.activeProductsCount").value(35));
    }

    @Test
    void returnsBadRequestWhenStartDateIsAfterEndDate() throws Exception {
        when(dashboardService.summary(END, START))
                .thenThrow(new BusinessRuleException("A data inicial não pode ser posterior à data final"));

        mockMvc.perform(get("/api/dashboard/summary").param("startDate", "2026-10-31").param("endDate", "2026-10-01"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("A data inicial não pode ser posterior à data final"));
    }

    @Test
    void returnsBadRequestWhenStartDateIsMissing() throws Exception {
        mockMvc.perform(get("/api/dashboard/summary").param("endDate", "2026-10-31"))
                .andExpect(status().isBadRequest());
        verify(dashboardService, never()).summary(any(), any());
    }

    @Test
    void returnsBadRequestWhenEndDateIsMissing() throws Exception {
        mockMvc.perform(get("/api/dashboard/summary").param("startDate", "2026-10-01"))
                .andExpect(status().isBadRequest());
        verify(dashboardService, never()).summary(any(), any());
    }

    @Test
    void returnsBadRequestWhenDateFormatIsInvalid() throws Exception {
        mockMvc.perform(get("/api/dashboard/summary").param("startDate", "01/10/2026").param("endDate", "2026-10-31"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/dashboard/summary").param("startDate", "2026-10-01").param("endDate", "abc"))
                .andExpect(status().isBadRequest());
        verify(dashboardService, never()).summary(any(), any());
    }
}
