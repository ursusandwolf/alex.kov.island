module com.island.app {
    requires com.island.engine;
    requires com.island.nature;
    requires com.island.simcity;
    
    requires com.fasterxml.jackson.databind;
    requires com.fasterxml.jackson.annotation;
    requires com.fasterxml.jackson.core;
    requires org.slf4j;
    requires static lombok;

    uses com.island.engine.core.SimulationPlugin;
}
