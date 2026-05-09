module com.island.engine {
    requires static lombok;
    requires org.slf4j;

    exports com.island.engine.core;
    exports com.island.engine.ecs;
    exports com.island.engine.event;
    exports com.island.engine.model;
    exports com.island.engine.scheduling;
    exports com.island.engine.service;
    exports com.island.util.common;
    exports com.island.util.math;
    exports com.island.util.sampling;
}
