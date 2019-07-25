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
    /*\// https://mvnrepository.com/artifact/org.codehaus.groovy/groovy-all
compile group: 'org.codehaus.groovy', name: 'groovy-all', version: '2.5.7', ext: 'pom'

*/
    compile(commonDep("org.codehaus.groovy", "groovy-all"))
    compileOnly(intellijPluginDep("gradle"))
    compileOnly(intellijCoreDep()) { includeJars("intellij-core") }
    compileOnly(intellijPluginDep("Groovy"))


    testCompile(kotlinStdlib())
    testCompile(project(":idea:idea-core"))
    testCompileOnly(intellijCoreDep()) { includeJars("intellij-core") }
    testCompile(project(":kotlin-test:kotlin-test-junit"))
    testCompileOnly(intellijPluginDep("Groovy"))
    testCompile(commonDep("junit:junit"))

    testCompileOnly(intellijDep())
}

sourceSets {
    "main" { projectDefault() }
    "test" { projectDefault() }
}

//tasks.withType<KotlinCompile> {
//    kotlinOptions.jvmTarget = "1.8"
//}