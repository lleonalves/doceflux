package com.doceflux.doceflux_api.dashboard.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record BusinessMetricsDto(
        @Schema(description = "Clientes cadastrados (estado atual, fora do filtro de período)", example = "120")
        long totalCustomers,
        @Schema(description = "Produtos ativos (estado atual, fora do filtro de período)", example = "35")
        long activeProductsCount) {
}
