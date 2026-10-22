package com.arametta.currencyconverter;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.Architectures.layeredArchitecture;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

/**
 * Architecture rules for the backend. They are the same rules written in
 * CLAUDE.md, but here they fail the build when someone breaks them.
 */
@AnalyzeClasses(
        packages = "com.arametta.currencyconverter",
        importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureTest {

    private static final String ROOT = "com.arametta.currencyconverter";

    @ArchTest
    static final ArchRule layers_are_respected = layeredArchitecture()
            .consideringOnlyDependenciesInLayers()
            .layer("Controller").definedBy(ROOT + ".controller..")
            .layer("Service").definedBy(ROOT + ".service..")
            .layer("Client").definedBy(ROOT + ".client..")
            .layer("Model").definedBy(ROOT + ".model..")
            .layer("Config").definedBy(ROOT + ".config..")
            .whereLayer("Controller").mayNotBeAccessedByAnyLayer()
            .whereLayer("Service").mayOnlyBeAccessedByLayers("Controller")
            .whereLayer("Client").mayOnlyBeAccessedByLayers("Service", "Controller")
            .whereLayer("Config").mayOnlyBeAccessedByLayers("Service")
            .because("each package has one job, see 'Dependency rules' in CLAUDE.md");

    @ArchTest
    static final ArchRule only_the_exception_handler_uses_the_client = noClasses()
            .that().resideInAPackage(ROOT + ".controller..")
            .and().doNotHaveSimpleName("GlobalExceptionHandler")
            .should().dependOnClassesThat().resideInAPackage(ROOT + ".client..")
            .because("controllers go through services; only GlobalExceptionHandler "
                    + "needs the client exceptions to map them to HTTP statuses");

    @ArchTest
    static final ArchRule client_depends_only_on_model = noClasses()
            .that().resideInAPackage(ROOT + ".client..")
            .should().dependOnClassesThat().resideInAnyPackage(
                    ROOT + ".controller..", ROOT + ".service..", ROOT + ".config..")
            .because("the client only talks to swop.cx and knows nothing about the rest of the app");

    @ArchTest
    static final ArchRule model_depends_on_nothing = noClasses()
            .that().resideInAPackage(ROOT + ".model..")
            .should().dependOnClassesThat().resideInAnyPackage(
                    ROOT + ".controller..", ROOT + ".service..",
                    ROOT + ".client..", ROOT + ".config..")
            .because("model classes are plain records shared by every layer");

    @ArchTest
    static final ArchRule only_client_and_config_use_rest_client = noClasses()
            .that().resideOutsideOfPackages(ROOT + ".client..", ROOT + ".config..")
            .should().dependOnClassesThat().haveFullyQualifiedName(
                    "org.springframework.web.client.RestClient")
            .because("only SwopClient calls swop.cx, using the RestClient bean from SwopClientConfig");

    @ArchTest
    static final ArchRule no_old_http_clients = noClasses()
            .should().dependOnClassesThat().haveFullyQualifiedName(
                    "org.springframework.web.client.RestTemplate")
            .orShould().dependOnClassesThat().haveFullyQualifiedName(
                    "org.springframework.web.reactive.function.client.WebClient")
            .because("the project uses RestClient for all HTTP calls");

    @ArchTest
    static final ArchRule only_the_exception_handler_builds_error_responses = noClasses()
            .that().doNotHaveFullyQualifiedName(ROOT + ".controller.GlobalExceptionHandler")
            .and().doNotHaveFullyQualifiedName(ROOT + ".model.ErrorResponse")
            .should().dependOnClassesThat().haveFullyQualifiedName(ROOT + ".model.ErrorResponse")
            .because("GlobalExceptionHandler is the one place that decides the error envelope");
}