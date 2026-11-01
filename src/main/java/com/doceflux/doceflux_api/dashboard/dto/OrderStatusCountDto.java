package com.doceflux.doceflux_api.dashboard.dto;

import com.doceflux.doceflux_api.order.domain.OrderStatus;
import io.swagger.v3.oas.annotations.media.Schema;

public record OrderStatusCountDto(
        @Schema(description = "Status atual do pedido")
        OrderStatus status,
        @Schema(description = "Quantidade de pedidos criados no período com este status", example = "5")
        long count) {
}
