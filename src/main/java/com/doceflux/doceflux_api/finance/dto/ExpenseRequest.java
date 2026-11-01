package com.doceflux.doceflux_api.finance.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ExpenseRequest(
        @NotBlank(message = "A descrição é obrigatória")
        @Size(max = 255, message = "A descrição deve ter no máximo 255 caracteres")
        String description,

        @NotNull(message = "O valor é obrigatório")
        @DecimalMin(value = "0.00", inclusive = false, message = "O valor deve ser maior que zero")
        @Digits(integer = 10, fraction = 2, message = "O valor deve ter no máximo 10 dígitos inteiros e 2 casas decimais")
        BigDecimal amount,

        @NotNull(message = "A data da despesa é obrigatória")
        @PastOrPresent(message = "A data da despesa não pode ser futura")
        LocalDate date,

        @Size(max = 60, message = "A categoria deve ter no máximo 60 caracteres")
        String category) {
}
