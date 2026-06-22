import org.springframework.boot.gradle.tasks.bundling.BootJar

group = "br.com.fiap.pos"

tasks.register<GradleBuild>("startStack") {
	description = "Starts all the necessary projects."
	group = JavaBasePlugin.BUILD_TASK_NAME
	tasks = subprojects.map { "${it.name}:bootRun" }.toList()
}

subprojects {
	tasks.withType<Jar> {
		enabled = false
	}
	tasks.withType<BootJar> {
		enabled = true
	}
	tasks.withType<Test> {
		useJUnitPlatform()
	}
}
