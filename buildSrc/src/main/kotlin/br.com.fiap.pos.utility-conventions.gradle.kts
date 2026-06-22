plugins {
    id("br.com.fiap.pos.common-conventions")
    id("io.freefair.lombok")
}

dependencies {
    implementation("org.mapstruct:mapstruct:${property("mapstructVersion")}")
    annotationProcessor("org.mapstruct:mapstruct-processor:${property("mapstructVersion")}")
}