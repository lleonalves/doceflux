package com.doceflux.doceflux_api.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @NotBlank(message = "O email é obrigatório")
        @Email(message = "O email informado é inválido")
        String email,

        @NotBlank(message = "A senha é obrigatória")
        String password) {
}
