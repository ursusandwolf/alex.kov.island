module com.island.simcity {
    requires com.island.engine;
    requires static lombok;
    requires org.slf4j;

    exports com.island.simcity;
    exports com.island.simcity.model;
    exports com.island.simcity.entities;
    exports com.island.simcity.entities.components;
    exports com.island.simcity.service;
    exports com.island.simcity.view;
}