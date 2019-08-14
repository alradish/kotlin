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
    compile(commonDep("com.github.cretz.kastree", "kastree-ast-jvm"))
    compile(commonDep("org.codehaus.groovy", "groovy-all"))
    compileOnly(intellijPluginDep("gradle"))
    compileOnly(intellijCoreDep()) { includeJars("intellij-core") }
    compileOnly(intellijPluginDep("Groovy"))


    testCompile(kotlinStdlib())
    testCompile(project(":idea:idea-core"))
    testCompile(commonDep("junit:junit"))
    testCompile(project(":kotlin-test:kotlin-test-junit"))

//    testCompile(project(":idea:idea-gradle").dependencyProject.testSourceSet.output)

    testCompileOnly(intellijCoreDep()) { includeJars("intellij-core") }
    testCompileOnly(intellijPluginDep("Groovy"))
    testCompileOnly(intellijPluginDep("gradle"))
    //implementation "org.jetbrains.kotlin:kotlin-reflect:$kotlin_version"
    testImplementation("org.jetbrains.kotlin", "kotlin-reflect")

    testCompileOnly(intellijDep())

    testRuntime(intellijPluginDep("gradle"))
    testRuntime(intellijPluginDep("Groovy"))
}

sourceSets {
    "main" { projectDefault() }
    "test" { projectDefault() }
}

//tasks.withType<KotlinCompile> {
//    kotlinOptions.jvmTarget = "1.8"
//}