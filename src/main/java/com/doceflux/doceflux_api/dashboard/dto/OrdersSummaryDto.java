package com.doceflux.doceflux_api.dashboard.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.List;

public record OrdersSummaryDto(
        @Schema(description = "Pedidos criados no período, em qualquer status", example = "42")
        long totalOrders,
        @Schema(description = "Pedidos criados no período, exceto cancelados", example = "40")
        long validOrdersCount,
        @Schema(description = "Soma do total dos pedidos não cancelados", example = "6000.00")
        BigDecimal totalOrdersAmount,
        @Schema(description = "totalOrdersAmount / validOrdersCount (2 casas, HALF_UP); zero sem pedidos válidos",
                example = "150.00")
        BigDecimal averageTicket,
        @Schema(description = "Distribuição por status atual, incluindo status com contagem zero")
        List<OrderStatusCountDto> ordersByStatus) {
}
