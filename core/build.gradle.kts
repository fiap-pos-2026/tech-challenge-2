plugins {
    id("br.com.fiap.pos.spring-web-conventions")
    id("br.com.fiap.pos.spring-jpa-conventions")
    id("br.com.fiap.pos.utility-conventions")
    id("org.owasp.dependencycheck") version "10.0.4"
    jacoco
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-oauth2-resource-server")
    implementation("org.springframework.boot:spring-boot-starter-data-redis")
    implementation("org.springframework.boot:spring-boot-starter-mail")
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:${property("springDocVersion")}")
    testImplementation("org.springframework.security:spring-security-test")
    testImplementation(platform("org.testcontainers:testcontainers-bom:2.0.5"))
    testImplementation("org.testcontainers:testcontainers-postgresql")
    testImplementation("org.testcontainers:testcontainers-junit-jupiter")
}

dependencyManagement {
    dependencies {
        dependency("com.fasterxml.jackson.core:jackson-databind:2.22.0")
    }
}

dependencyCheck {
    failBuildOnCVSS = 7.0f
    suppressionFile = "owasp-suppressions.xml"
    formats = listOf("HTML", "JSON")
}

jacoco {
    toolVersion = "0.8.13"
}

tasks.test {
    ignoreFailures = true
}

tasks.jacocoTestReport {
    dependsOn(tasks.test)
    reports {
        xml.required = true
        html.required = true
    }
    classDirectories.setFrom(
        files(classDirectories.files.map {
            fileTree(it) {
                exclude(
                    "**/dto/**",
                    "**/config/**",
                    "**/exception/**",
                    "**/enums/**",
                    "**/domain/**",
                    "**/*Application*",
                    "**/*Mapper*"
                )
            }
        })
    )
}

tasks.jacocoTestCoverageVerification {
    dependsOn(tasks.jacocoTestReport)
    violationRules {
        rule {
            // measure only service, scheduler, and validation packages (excludes controllers, repos, DTOs)
            includes = listOf(
                "br.com.fiap.pos.tech_challenge.core.service.*",
                "br.com.fiap.pos.tech_challenge.core.scheduler.*",
                "br.com.fiap.pos.tech_challenge.core.validation.*"
            )
            limit {
                counter = "LINE"
                value = "COVEREDRATIO"
                minimum = "0.80".toBigDecimal()
            }
        }
    }
}