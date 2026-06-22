import org.apache.tools.ant.filters.ReplaceTokens

plugins {
    java
}

group = "br.com.fiap.pos"

repositories {
    mavenCentral()
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of("${project.property("javaVersion")}")
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}

tasks.withType<JavaCompile> {
    options.compilerArgs.addAll(listOf("-Xlint:deprecation", "-Xlint:unchecked"))
}

tasks.processResources {
    filter<ReplaceTokens>("tokens" to mapOf(
        "version" to project.version,
        "gradle_version" to gradle.gradleVersion
    ))
}