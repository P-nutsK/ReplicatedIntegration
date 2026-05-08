plugins {
    `java-library`
    kotlin("jvm")
}

repositories {
    mavenCentral()
}

dependencies {
    // Match the oldest supported loader classpath: 1.18.2 resolves slf4j-api to 1.8.0-beta4.
    compileOnly(libs.slf4j.api)
    api(libs.gson)
    testImplementation(kotlin("test"))
}

java.toolchain {
    languageVersion = JavaLanguageVersion.of(17)
}
