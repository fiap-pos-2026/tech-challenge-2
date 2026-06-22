// Loading the root gradle.properties file into the buildSrc scope
pluginManagement {
    val properties = java.util.Properties()

    rootDir.parentFile.resolve("gradle.properties").inputStream().use {
        properties.load(it)
    }

    properties.forEach { (key, value) ->
        settings.extensions.extraProperties.set(key.toString(), value)
    }

    gradle.rootProject {
        properties.forEach { (key, value) ->
            extensions.extraProperties.set(key.toString(), value)
        }
    }
}

rootProject.name = "buildSrc"