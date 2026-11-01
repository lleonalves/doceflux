package com.doceflux.doceflux_api.order.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

public record OrderRequest(
        @NotNull(message = "O cliente é obrigatório")
        UUID customerId,

        @Size(max = 2000, message = "A descrição deve ter no máximo 2000 caracteres")
        String description,

        @NotEmpty(message = "O pedido deve ter ao menos um item")
        List<@NotNull(message = "O item não pode ser nulo") @Valid OrderItemRequest> items) {
}
