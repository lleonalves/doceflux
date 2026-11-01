package com.doceflux.doceflux_api.order.dto;

import com.doceflux.doceflux_api.order.domain.OrderStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

public record OrderStatusRequest(
        @Schema(description = "Novo status do pedido", example = "IN_PRODUCTION")
        @NotNull(message = "O status é obrigatório")
        OrderStatus status) {
}
