package com.forecastpro.config;

import jakarta.ejb.ApplicationException;

@ApplicationException(rollback = false)
public class BusinessException extends RuntimeException {

    public BusinessException(String message) {
        super(message);
    }

    public BusinessException(String message, Throwable cause) {
        super(message, cause);
    }
}
