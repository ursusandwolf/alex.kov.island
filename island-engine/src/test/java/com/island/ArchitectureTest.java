package com.island;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

public class ArchitectureTest {

    @Test
    void utilShouldNotDependOnNature() {
        JavaClasses importedClasses = new ClassFileImporter().importPackages("com.island");

        ArchRule rule = noClasses().that().resideInAPackage("com.island.util..")
            .should().dependOnClassesThat()
            .resideInAnyPackage("com.island.nature..", "com.island.simcity..");

        rule.check(importedClasses);
    }
}
