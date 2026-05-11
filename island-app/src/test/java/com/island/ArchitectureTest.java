package com.island;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

public class ArchitectureTest {

    private final JavaClasses classes = new ClassFileImporter().importPackages("com.island");

    @Test
    void engineShouldNotDependOnDomain() {
        noClasses().that().resideInAPackage("com.island.engine..")
            .should().dependOnClassesThat()
            .resideInAnyPackage("com.island.nature..", "com.island.simcity..")
            .check(classes);
    }

    @Test
    void utilShouldNotDependOnDomain() {
        noClasses().that().resideInAPackage("com.island.util..")
            .should().dependOnClassesThat()
            .resideInAnyPackage("com.island.nature..", "com.island.simcity..")
            .check(classes);
    }

    @Test
    void natureAndSimCityShouldNotDependOnEachOther() {
        noClasses().that().resideInAPackage("com.island.nature..")
            .should().dependOnClassesThat()
            .resideInAnyPackage("com.island.simcity..")
            .check(classes);

        noClasses().that().resideInAPackage("com.island.simcity..")
            .should().dependOnClassesThat()
            .resideInAnyPackage("com.island.nature..")
            .check(classes);
    }

    @Test
    void pluginsShouldNotUseEngineInternals() {
        noClasses().that().resideInAnyPackage("com.island.nature..", "com.island.simcity..")
            .should().dependOnClassesThat()
            .resideInAnyPackage("com.island.engine.internal..")
            .check(classes);

        noClasses().that().resideInAnyPackage("com.island.nature..", "com.island.simcity..")
            .should().dependOnClassesThat()
            .haveSimpleName("CellProcessor")
            .check(classes);
            
        // Check for InternalEngine annotation (requires RetentionPolicy.CLASS)
        noClasses().that().resideInAnyPackage("com.island.nature..", "com.island.simcity..")
            .should().dependOnClassesThat()
            .areAnnotatedWith("com.island.engine.core.InternalEngine")
            .check(classes);
    }
}
