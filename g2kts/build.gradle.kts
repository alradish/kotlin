plugins {
    kotlin("jvm")
    id("jps-compatible")
}

//group = "org.jetbrains.kotlin"
//version = "1.3-SNAPSHOT"

repositories {
    mavenCentral()
//    maven { setUrl("https://repo.gradle.org/gradle/libs-releases-local") }
}

dependencies {
    compile(kotlinStdlib())
    compile(project(":idea:idea-core"))
    compile(commonDep("com.github.cretz.kastree", "kastree-ast-jvm"))
    compile(commonDep("org.codehaus.groovy", "groovy-all"))
    compileOnly(intellijPluginDep("gradle"))
    compileOnly(intellijCoreDep()) { includeJars("intellij-core") }
    compileOnly(intellijPluginDep("Groovy"))

    implementation(gradleKotlinDsl())
//    compile(project(":idea:idea-gradle"))

    compile(project(":idea:kotlin-gradle-tooling"))

    Platform[192].orHigher {
        compileOnly(intellijPluginDep("java"))
        testCompileOnly(intellijPluginDep("java"))
        testRuntimeOnly(intellijPluginDep("java"))
    }


    testCompile(projectTests(":idea:idea-gradle"))
//    testCompile(projectTests(":idea"))
    testCompile(projectTests(":idea:idea-test-framework"))

    testCompile(intellijPluginDep("gradle"))
    Platform[193].orHigher {
        testCompile(intellijPluginDep("gradle-java"))
    }
    testCompileOnly(intellijPluginDep("Groovy"))
    testCompileOnly(intellijDep())

    testCompile(project(":idea:idea-native")) { isTransitive = false }
    testCompile(project(":idea:idea-gradle-native")) { isTransitive = false }
//    testRuntime(project(":kotlin-native:kotlin-native-library-reader")) { isTransitive = false }
//    testRuntime(project(":kotlin-native:kotlin-native-utils")) { isTransitive = false }
    testRuntime(project(":idea:idea-new-project-wizard"))

    testRuntime(project(":kotlin-reflect"))
    testRuntime(project(":idea:idea-jvm"))
    testRuntime(project(":idea:idea-android"))
    testRuntime(project(":plugins:kapt3-idea"))
    testRuntime(project(":plugins:android-extensions-ide"))
    testRuntime(project(":plugins:lint"))
    testRuntime(project(":sam-with-receiver-ide-plugin"))
    testRuntime(project(":allopen-ide-plugin"))
    testRuntime(project(":noarg-ide-plugin"))
    testRuntime(project(":kotlin-scripting-idea"))
    testRuntime(project(":kotlinx-serialization-ide-plugin"))
//
//    // TODO: the order of the plugins matters here, consider avoiding order-dependency
//    testRuntime(intellijPluginDep("junit"))
//    testRuntime(intellijPluginDep("testng"))
//    testRuntime(intellijPluginDep("properties"))
//    testRuntime(intellijPluginDep("gradle"))
//    Platform[193].orHigher {
//        testRuntime(intellijPluginDep("gradle-java"))
//    }
//    testRuntime(intellijPluginDep("Groovy"))
//    testRuntime(intellijPluginDep("coverage"))
//    if (Ide.IJ()) {
//        testRuntime(intellijPluginDep("maven"))
//
//        if (Ide.IJ201.orHigher()) {
//            testRuntime(intellijPluginDep("repository-search"))
//        }
//    }
//    testRuntime(intellijPluginDep("android"))
//    testRuntime(intellijPluginDep("smali"))
//
//    if (Ide.AS36.orHigher()) {
//        testRuntime(intellijPluginDep("android-layoutlib"))
//    }

    testCompile(kotlinStdlib())
    testCompile(project(":idea:idea-core"))
    testCompile(commonDep("junit:junit"))
    testCompile(project(":kotlin-test:kotlin-test-junit"))

//    testCompile(project(":idea:idea-gradle").dependencyProject.testSourceSet.output)

    testCompileOnly(intellijCoreDep()) { includeJars("intellij-core") }
    testCompileOnly(intellijPluginDep("gradle"))
    //implementation "org.jetbrains.kotlin:kotlin-reflect:$kotlin_version"
    testImplementation("org.jetbrains.kotlin", "kotlin-reflect")


    testRuntime(intellijPluginDep("gradle"))
    testRuntime(intellijPluginDep("Groovy"))

//    testImplementation(project(":idea:idea-gradle"))
}

sourceSets {
    "main" { projectDefault() }
    "test" { projectDefault() }
}

testsJar()

//tasks.withType<KotlinCompile> {
//    kotlinOptions.jvmTarget = "1.8"
//}