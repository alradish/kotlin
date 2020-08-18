plugins {
    id("java")
}
group = "test"
version = "1.0-SNAPSHOT"
sourceCompatibility = 1.8
repositories {
    mavenCentral()
}
dependencies {
    testCompile(group = "junit", name = "junit", version = "4.12")
}