package com.doceflux.doceflux_api.shared;

public class InvalidInviteCodeException extends RuntimeException {

    public InvalidInviteCodeException() {
        super("Código de convite inválido ou não informado");
    }
}
