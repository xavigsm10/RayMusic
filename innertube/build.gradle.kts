import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    kotlin("plugin.serialization") version "2.2.21"
    kotlin("jvm")
}

kotlin {
    compilerOptions.jvmTarget = JvmTarget.JVM_11
}

java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}

dependencies {
    val ktorVersion = "3.4.1"
    implementation("io.ktor:ktor-client-core:$ktorVersion")
    implementation("io.ktor:ktor-client-okhttp:$ktorVersion")
    implementation("io.ktor:ktor-client-content-negotiation:$ktorVersion")
    implementation("io.ktor:ktor-serialization-kotlinx-json:$ktorVersion")
    implementation("io.ktor:ktor-client-encoding:$ktorVersion")
    implementation("org.brotli:dec:0.1.2")
    implementation("com.github.mostafaalagamy:MetrolistExtractor:8773f0d") {
        exclude(group = "com.google.protobuf", module = "protobuf-java")
    }
    testImplementation("junit:junit:4.13.2")
}
