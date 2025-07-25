package com.samdoherty.aggregator.api.exception;

import lombok.Builder;

/**
 * Standardised API error object
 */
@Builder
public record ApiError(int status,
                       String error,
                       String message,
                       String path) {
}
