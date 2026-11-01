package com.doceflux.doceflux_api.dashboard.controller;

import com.doceflux.doceflux_api.dashboard.dto.DashboardSummaryResponse;
import com.doceflux.doceflux_api.dashboard.service.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@Tag(name = "Dashboard")
@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @Operation(summary = "Resumo gerencial do período",
            description = "Consolida indicadores financeiros e de pedidos do período informado (inclusive nas duas "
                    + "pontas) e métricas gerais atuais de clientes e produtos ativos.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Resumo retornado com sucesso"),
            @ApiResponse(responseCode = "400",
                    description = "Datas ausentes, em formato inválido ou período inconsistente",
                    content = @Content)
    })
    @GetMapping("/summary")
    public DashboardSummaryResponse summary(
            @Parameter(description = "Data inicial (YYYY-MM-DD)", required = true, example = "2026-10-01")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @Parameter(description = "Data final (YYYY-MM-DD)", required = true, example = "2026-10-31")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return dashboardService.summary(startDate, endDate);
    }
}
