package com.raulbolivar.exception;

public class BusinessException extends RuntimeException {

    private final String domain;
    private final String code;

    public BusinessException(String domain, String code, String message) {
        super(message);
        this.domain = domain;
        this.code = code;
    }

    public String getDomain() {
        return domain;
    }

    public String getCode() {
        return code;
    }
}