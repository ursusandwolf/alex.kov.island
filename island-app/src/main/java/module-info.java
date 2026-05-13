module com.island.app {
    requires com.island.engine;
    requires com.island.nature;
    requires com.island.simcity;
    
    requires com.fasterxml.jackson.databind;
    requires com.fasterxml.jackson.annotation;
    requires com.fasterxml.jackson.core;
    requires org.slf4j;
    requires static lombok;

    // Spring Boot
    requires spring.boot;
    requires spring.boot.autoconfigure;
    requires spring.context;
    requires spring.web;
    requires spring.webmvc;
    requires spring.beans;
    requires spring.core;
    requires spring.messaging;
    requires spring.websocket;
    requires jakarta.annotation;
    requires jakarta.validation;

    opens com.island to spring.core, spring.beans, spring.context;
    opens com.island.config to spring.core, spring.beans, spring.context, com.fasterxml.jackson.databind, spring.messaging;
    opens com.island.service to spring.core, spring.beans, spring.context, spring.messaging;
    opens com.island.controller to spring.core, spring.beans, spring.context, spring.web, spring.messaging, com.fasterxml.jackson.databind;
    
    uses com.island.engine.core.SimulationPlugin;
}
