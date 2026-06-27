plugins {
    id("br.com.fiap.pos.spring-web-conventions")
    id("br.com.fiap.pos.spring-jpa-conventions")
    id("br.com.fiap.pos.utility-conventions")
    id("org.owasp.dependencycheck") version "10.0.4"
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-oauth2-resource-server")
    implementation("org.springframework.boot:spring-boot-starter-mail")
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:${property("springDocVersion")}")
    testImplementation("org.springframework.security:spring-security-test")
    testImplementation("org.testcontainers:postgresql")
}

dependencyCheck {
    failBuildOnCVSS = 7.0f
    suppressionFile = "owasp-suppressions.xml"
    formats = listOf("HTML", "JSON")
}