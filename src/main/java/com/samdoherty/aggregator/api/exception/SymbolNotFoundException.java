package com.samdoherty.aggregator.api.exception;

public class SymbolNotFoundException extends RuntimeException {

    public SymbolNotFoundException(String message) {
        super(message);
    }
}
