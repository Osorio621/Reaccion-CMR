package com.reactivosdelvalle.crm_api.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class AppException extends RuntimeException {

    private final HttpStatus status;
    private final String errorKey;

    public AppException(String message, HttpStatus status) {
        super(message);
        this.status = status;
        this.errorKey = resolveErrorKey(status);
    }

    public AppException(String message, HttpStatus status, String errorKey) {
        super(message);
        this.status = status;
        this.errorKey = errorKey;
    }

    private String resolveErrorKey(HttpStatus status) {
        if (status == HttpStatus.NOT_FOUND) {
            return "RECURSO_NO_ENCONTRADO";
        } else if (status == HttpStatus.BAD_REQUEST) {
            return "SOLICITUD_INCORRECTA";
        } else if (status == HttpStatus.FORBIDDEN) {
            return "ACCESO_DENEGADO";
        } else if (status == HttpStatus.UNAUTHORIZED) {
            return "NO_AUTENTICADO";
        }
        return "ERROR_INTERNO";
    }
}
