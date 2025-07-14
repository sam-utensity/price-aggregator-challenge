package com.samdoherty.aggregator.domain.model;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;

/**
 * Standardised
 */
@Getter
@Builder
@EqualsAndHashCode
public class Instrument {
    /**
     * Strictly speaking, this is not yet needed. Is here, for example, purposes of extendability
     */
    private String exchange;
    private String base;
    private String quote;

    /**
     * Used purely to ensure price scaling is correct when presented to the API
     */
    @EqualsAndHashCode.Exclude
    private int scale;
}
