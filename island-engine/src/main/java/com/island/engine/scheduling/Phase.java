package com.island.engine.scheduling;

import com.island.engine.core.EngineAPI;

@EngineAPI
public enum Phase {
    PREPARE,
    SIMULATION,
    POSTPROCESS
}