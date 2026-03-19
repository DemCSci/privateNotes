import org.gradle.api.tasks.compile.JavaCompile
import org.jetbrains.intellij.platform.gradle.TestFrameworkType

plugins {
    id("java")
    id("org.jetbrains.intellij.platform") version "2.10.2"
}

group = "com.theblind.privatenotes"
version = "1.8.10"

repositories {
    mavenCentral()
    intellijPlatform {
        defaultRepositories()
    }
}

dependencies {
    implementation("com.google.code.gson:gson:2.12.1")
    implementation(files("resources/lib/hutool-all-5.7.13.jar"))

    intellijPlatform {
        intellijIdea("2025.3.2")
        testFramework(TestFrameworkType.Platform)
    }
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

sourceSets {
    main {
        java.setSrcDirs(listOf("src"))
        resources.setSrcDirs(listOf("resources"))
    }
    test {
        java.setSrcDirs(listOf("test"))
    }
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.release.set(21)
}

tasks.processResources {
    exclude("lib/**")
}

tasks.buildSearchableOptions {
    enabled = false
}

intellijPlatform {
    pluginConfiguration {
        version = project.version.toString()
        ideaVersion {
            sinceBuild = "253"
        }
    }
}
