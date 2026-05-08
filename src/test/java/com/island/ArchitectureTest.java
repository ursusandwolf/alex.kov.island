package com.island;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.Test;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

class ArchitectureTest {

    private final JavaClasses importedClasses = new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages("com.island");

    @Test
    void engineShouldNotDependOnDomainPackages() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("..engine..")
                .should().dependOnClassesThat()
                .resideInAnyPackage("..nature..", "..simcity..");

        rule.check(importedClasses);
    }

    @Test
    void utilShouldNotDependOnDomainPackages() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("..util..")
                .should().dependOnClassesThat()
                .resideInAnyPackage("..nature..", "..simcity..");

        rule.check(importedClasses);
    }
}