package com.doceflux.doceflux_api.order.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.UUID;

public record OrderItemRequest(
        UUID productId,

        @Size(max = 120, message = "O nome do item deve ter no máximo 120 caracteres")
        String name,

        @Size(max = 2000, message = "A descrição do item deve ter no máximo 2000 caracteres")
        String description,

        @NotNull(message = "A quantidade é obrigatória")
        @Min(value = 1, message = "A quantidade deve ser maior que zero")
        Integer quantity,

        @DecimalMin(value = "0.00", inclusive = false, message = "O preço unitário deve ser maior que zero")
        @Digits(integer = 10, fraction = 2, message = "O preço unitário deve ter no máximo 10 dígitos inteiros e 2 casas decimais")
        BigDecimal unitPrice) {
}
