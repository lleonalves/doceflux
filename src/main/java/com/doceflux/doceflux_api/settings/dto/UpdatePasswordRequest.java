package com.doceflux.doceflux_api.settings.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdatePasswordRequest(
        @NotBlank(message = "A senha atual é obrigatória")
        String currentPassword,

        @NotBlank(message = "A nova senha é obrigatória")
        @Size(min = 8, max = 100, message = "A nova senha deve ter entre 8 e 100 caracteres")
        String newPassword) {
}
