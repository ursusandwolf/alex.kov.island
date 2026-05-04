package com.island.util;

import lombok.Value;

/**
 * Context for sampling operations, encapsulating limit and randomness.
 */
@Value
public class SamplingContext {
    int limit;
    RandomProvider random;
}
