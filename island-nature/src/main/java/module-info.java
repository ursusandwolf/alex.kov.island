module com.island.nature {
    requires com.island.engine;
    requires static lombok;
    requires org.slf4j;

    exports com.island.nature;
    exports com.island.nature.config;
    exports com.island.nature.model;
    exports com.island.nature.view;
    exports com.island.nature.entities.core;
}