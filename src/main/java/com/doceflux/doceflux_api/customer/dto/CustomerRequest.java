package com.doceflux.doceflux_api.customer.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CustomerRequest(
        @NotBlank(message = "O nome é obrigatório")
        @Size(max = 120, message = "O nome deve ter no máximo 120 caracteres")
        String name,

        @NotBlank(message = "O telefone é obrigatório")
        @Size(max = 20, message = "O telefone deve ter no máximo 20 caracteres")
        String phone,

        @Email(message = "O email informado é inválido")
        @Size(max = 160, message = "O email deve ter no máximo 160 caracteres")
        String email,

        @Size(max = 255, message = "O endereço deve ter no máximo 255 caracteres")
        String address) {
}
