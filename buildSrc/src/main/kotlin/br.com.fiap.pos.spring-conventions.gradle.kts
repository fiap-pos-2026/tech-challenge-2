plugins {
    id("br.com.fiap.pos.common-conventions")
    id("io.spring.dependency-management")
    id("org.springframework.boot")
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter")
    annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
}

dependencyManagement {
    imports {
        mavenBom("org.springframework.cloud:spring-cloud-dependencies:${project.property("springCloudVersion")}")
    }
}

tasks.named("compileJava") {
    inputs.files(tasks.named("processResources"))
}