import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm")
    id("jps-compatible")
}

//group = "org.jetbrains.kotlin"
//version = "1.3-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    compile(kotlinStdlib())
    compile(project(":idea:idea-core"))
    compileOnly(intellijCoreDep()) { includeJars("intellij-core") }
    compileOnly(intellijPluginDep("Groovy"))
}

sourceSets {
    "main" { projectDefault() }
    "test" { projectDefault() }
}

//tasks.withType<KotlinCompile> {
//    kotlinOptions.jvmTarget = "1.8"
//}