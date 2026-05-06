package com.island.util.sampling;

import lombok.Value;
import com.island.util.common.RandomProvider;

/**
 * Context for sampling operations, encapsulating limit and randomness.
 */
@Value
public class SamplingContext {
    int limit;
    RandomProvider random;
}