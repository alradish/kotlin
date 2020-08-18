description = "Kotlin Gradle Tooling support"

plugins {
    kotlin("jvm")
    id("jps-compatible")
}

jvmTarget = "1.6"

//repositories {
//    maven { url = uri("https://repo.gradle.org/gradle/libs-releases-local") }
//}

dependencies {
    compile(kotlinStdlib())

    compile("org.gradle","gradle-kotlin-dsl-provider-plugins","6.0.1")
//    gradleApi()

    compileOnly(intellijPluginDep("gradle"))
    compileOnly(intellijDep()) { includeJars("slf4j-api-1.7.25") }
    Platform[193].orLower {
        compile(project(":idea:idea-gradle-tooling-api"))
    }
}

sourceSets {
    "main" { projectDefault() }
    "test" {}
}

runtimeJar()

sourcesJar()

javadocJar()

apply(from = "$rootDir/gradle/kotlinPluginPublication.gradle.kts")
