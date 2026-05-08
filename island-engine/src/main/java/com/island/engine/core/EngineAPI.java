package com.island.engine.core;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Marks a class or interface as part of the public, stable Engine API.
 */
@Retention(RetentionPolicy.SOURCE)
public @interface EngineAPI {
}
