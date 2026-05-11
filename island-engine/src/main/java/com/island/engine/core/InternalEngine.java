package com.island.engine.core;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Marks a class or interface as an internal part of the engine that may change without notice.
 */
@Retention(RetentionPolicy.CLASS)
public @interface InternalEngine {
}
