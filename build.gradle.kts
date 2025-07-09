import java.net.URL
import java.nio.file.Files

val jarDir = layout.buildDirectory.dir("libs/minecraftJars")

plugins {
    id("java")
}

java {
    sourceSets["main"].java.srcDirs("minecraft/src")
}

tasks.register("downloadJars") {
    doLast {
        // Path to the dependencies.json file
        val jsonFile = file("minecraft/src/dependencies.json")
        if (!jsonFile.exists()) {
            throw GradleException("dependencies.json file not found at ${jsonFile.absolutePath}")
        }

        // Read the JSON file as a plain string
        val jsonContent = jsonFile.readText()

        // Extract URLs manually
        val jarUrls = Regex("\"url\":\\s*\"(https?://[^\"]+)\"")
            .findAll(jsonContent)
            .map { it.groupValues[1] }
            .toList()

        val targetDir = jarDir.get().asFile
        if (!targetDir.exists()) targetDir.mkdirs()

        jarUrls.forEach { jarUrl ->
            val fileName = jarUrl.substringAfterLast("/")
            val targetFile = targetDir.resolve(fileName)

            if (!targetFile.exists()) {
                println("Downloading $fileName...")
                URL(jarUrl).openStream().use { input ->
                    Files.copy(input, targetFile.toPath())
                }
                println("Saved to ${targetFile.absolutePath}")
            } else {
                println("$fileName already exists, skipping.")
            }
        }
    }
}

repositories {
    mavenCentral()
    maven {
        name = "Fabric"
        url = uri("https://maven.fabricmc.net/")
    }
}

dependencies {
    // Guard clause: Only include files if they exist
    val externalJars = jarDir.get().asFile.takeIf { it.exists() }?.listFiles()?.toList() ?: emptyList()
    if (externalJars.isNotEmpty()) {
        implementation(files(externalJars))
    }
    implementation("net.fabricmc:fabric-loader:0.16.14")
    implementation("com.google.code.findbugs:jsr305:3.0.2")
}

// Ensure build depends on downloading the JARs
tasks.named("build") {
    dependsOn("downloadJars")
}