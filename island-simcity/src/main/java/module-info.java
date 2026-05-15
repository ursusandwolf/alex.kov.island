module com.island.simcity {
    requires com.island.engine;
    requires static lombok;
    requires org.slf4j;
    requires spring.context;
    requires spring.beans;

    exports com.island.simcity;
    exports com.island.simcity.model;
    exports com.island.simcity.entities;
    exports com.island.simcity.entities.components;
    exports com.island.simcity.service;

    opens com.island.simcity.model;

    provides com.island.engine.core.SimulationPlugin with com.island.simcity.SimCityPlugin;
}