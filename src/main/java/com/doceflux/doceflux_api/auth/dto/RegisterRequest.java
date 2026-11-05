package com.doceflux.doceflux_api.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record RegisterRequest(
        @NotBlank(message = "O nome da loja é obrigatório")
        @Size(max = 150, message = "O nome da loja deve ter no máximo 150 caracteres")
        String storeName,

        @NotBlank(message = "O gestor principal é obrigatório")
        @Size(max = 120, message = "O gestor principal deve ter no máximo 120 caracteres")
        String mainManager,

        @NotBlank(message = "O gestor responsável é obrigatório")
        @Size(max = 120, message = "O gestor responsável deve ter no máximo 120 caracteres")
        String responsibleManager,

        @NotNull(message = "A data de início da empresa é obrigatória")
        LocalDate companyStartDate,

        @NotBlank(message = "O email é obrigatório")
        @Email(message = "O email informado é inválido")
        @Size(max = 150, message = "O email deve ter no máximo 150 caracteres")
        String email,

        @NotBlank(message = "A senha é obrigatória")
        @Size(min = 6, max = 100, message = "A senha deve ter entre 6 e 100 caracteres")
        String password) {
}
