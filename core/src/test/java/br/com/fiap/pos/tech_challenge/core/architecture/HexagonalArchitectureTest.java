package br.com.fiap.pos.tech_challenge.core.architecture;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.net.URISyntaxException;
import java.nio.file.Path;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

class HexagonalArchitectureTest {

    private static final String DOMAIN = "..core.domain..";
    private static final String APPLICATION = "..core.application..";
    private static final String PORTS = "..core.application.port..";
    private static final String WEB = "..core.web..";
    private static final String INFRA = "..core.infrastructure..";

    private static final String AUTHENTICATION_SERVICE =
            "br.com.fiap.pos.tech_challenge.core.application.AuthenticationService";

    private static JavaClasses core;

    @BeforeAll
    static void importMainClasses() throws URISyntaxException {
        Path testClasses = Path.of(HexagonalArchitectureTest.class.getProtectionDomain()
                .getCodeSource().getLocation().toURI());
        core = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPath(testClasses.resolveSibling("main"));
    }

    @Test
    void domainDependsOnNoOtherLayer() {
        check(noClasses().that().resideInAPackage(DOMAIN)
                .should().dependOnClassesThat().resideInAnyPackage(APPLICATION, WEB, INFRA));
    }

    @Test
    void domainIsFrameworkFree() {
        check(noClasses().that().resideInAPackage(DOMAIN)
                .should().dependOnClassesThat().resideInAnyPackage(
                        "org.springframework..",
                        "jakarta.persistence..",
                        "org.mapstruct..",
                        "com.fasterxml.jackson.."));
    }

    @Test
    void applicationDoesNotDependOnWeb() {
        check(noClasses().that().resideInAPackage(APPLICATION)
                .should().dependOnClassesThat().resideInAPackage(WEB));
    }

    @Test
    void applicationDoesNotDependOnInfrastructure() {
        check(noClasses().that().resideInAPackage(APPLICATION)
                .and().doNotHaveFullyQualifiedName(AUTHENTICATION_SERVICE)
                .should().dependOnClassesThat().resideInAPackage(INFRA));
    }

    @Test
    void outboundPortsDependOnNoAdapter() {
        check(noClasses().that().resideInAPackage(PORTS)
                .should().dependOnClassesThat().resideInAnyPackage(WEB, INFRA));
    }

    @Test
    void webDoesNotDependOnPersistenceOrConfig() {
        check(noClasses().that().resideInAPackage(WEB)
                .should().dependOnClassesThat().resideInAnyPackage(
                        "..core.infrastructure.persistence..",
                        "..core.infrastructure.config.."));
    }

    private static void check(ArchRule rule) {
        rule.check(core);
    }
}
