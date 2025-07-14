package com.samdoherty.aggregator.api.exception;

/**
 * When api requests are made for currency pairs, we have not configured/ no pricing exists for;
 * we throw this exception
 */
public class SymbolNotFoundException extends RuntimeException {

    public SymbolNotFoundException(String message) {
        super(message);
    }
}
