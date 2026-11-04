package com.doceflux.doceflux_api.product.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

public record ProductActiveRequest(
        @Schema(description = "Indica se o produto está ativo no catálogo", example = "false")
        @NotNull(message = "O campo active é obrigatório")
        Boolean active) {
}
