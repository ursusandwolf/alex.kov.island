module com.island.nature {
    requires com.island.engine;
    requires static lombok;
    requires org.slf4j;

    exports com.island.nature;
    exports com.island.nature.config;
    exports com.island.nature.view;
    exports com.island.nature.entities.core;
    exports com.island.nature.model;

    opens com.island.nature.model;

    provides com.island.engine.core.SimulationPlugin with com.island.nature.NaturePlugin;
}