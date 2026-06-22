plugins {
    id("br.com.fiap.pos.spring-web-conventions")
    id("br.com.fiap.pos.spring-jpa-conventions")
    id("br.com.fiap.pos.utility-conventions")
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-oauth2-resource-server")
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:${property("springDocVersion")}")
    testImplementation("org.springframework.security:spring-security-test")
}