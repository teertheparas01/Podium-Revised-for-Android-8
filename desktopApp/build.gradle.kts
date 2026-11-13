@file:Suppress("DEPRECATION")

import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
    id("application")
}

group = "app.podium"
version = "0.1.0-desktop"

dependencies {
    // JavaFX explicit artifacts (on Maven Central)
    implementation("org.openjfx:javafx-controls:21.0.1:win")
    implementation("org.openjfx:javafx-fxml:21.0.1:win")
    implementation("org.openjfx:javafx-media:21.0.1:win")
    implementation("org.openjfx:javafx-base:21.0.1:win")
    implementation("org.openjfx:javafx-graphics:21.0.1:win")
    implementation("org.openjfx:javafx-web:21.0.1:win")
    
    // Shared Kotlin core modules (ADR-012)
    implementation(project(":core:model"))
    implementation(project(":core:common"))
    implementation(project(":core:lyrics"))
    implementation(project(":sources:api"))
    implementation(project(":player:api"))
    
    // Serialization
    implementation(libs.kotlinx.serialization.json)
    
    // Coroutines
    implementation(libs.kotlinx.coroutines.core)
    
    // OkHttp for network
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    
    // JNA for Windows APIs (SMTC, media keys)
    implementation(libs.jna)
    implementation(libs.jna.platform)
    
    // Logging
    implementation("org.slf4j:slf4j-simple:2.0.13")
    
    // Future YouTube Music integration (commented out for MVP)
    // implementation(libs.webview2)
    // implementation(libs.graaljs)
    // implementation(libs.graaljs.scriptengine)
    // implementation(libs.winrt)
    // implementation(libs.media3.exoplayer)
    
    // Test
    testImplementation(libs.kotlin.test)
    testImplementation(libs.kotlin.test.junit)
    testImplementation(libs.turbine)
}

kotlin {
    jvmToolchain(17)
    
    sourceSets {
        named("main")
        named("test")
    }
}

application {
    mainClass.set("app.podium.desktop.DesktopApp")
}

// Configure JVM args for JavaFX modules on the run task
tasks.named<org.gradle.api.tasks.JavaExec>("run") {
    // Add JavaFX modules - module path will be set via JAVA_OPTS or gradle.properties for now
    // TODO: Properly configure module path with JavaFX JARs
    jvmArgs = listOf(
        "--add-modules", "javafx.controls,javafx.fxml,javafx.media,javafx.base,javafx.graphics,javafx.web"
    )
}

kotlin {
    jvmToolchain(17)
    
    sourceSets {
        named("main")
        named("test")
    }
}

application {
    mainClass.set("app.podium.desktop.DesktopApp")
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    compilerOptions {
        freeCompilerArgs = listOf("-Xopt-in=kotlin.RequiresOptIn", "-Xjsr305=strict")
        jvmTarget = JvmTarget.JVM_17
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}