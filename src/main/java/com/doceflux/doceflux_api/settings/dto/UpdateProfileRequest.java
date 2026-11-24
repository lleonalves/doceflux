package com.doceflux.doceflux_api.settings.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateProfileRequest(
        @NotBlank(message = "O nome da loja é obrigatório")
        @Size(max = 150, message = "O nome da loja deve ter no máximo 150 caracteres")
        String storeName,

        @NotBlank(message = "O gestor principal é obrigatório")
        @Size(max = 120, message = "O gestor principal deve ter no máximo 120 caracteres")
        String mainManager,

        @NotBlank(message = "O gestor responsável é obrigatório")
        @Size(max = 120, message = "O gestor responsável deve ter no máximo 120 caracteres")
        String responsibleManager) {
}
