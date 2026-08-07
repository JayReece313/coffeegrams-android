import org.jetbrains.kotlin.gradle.dsl.JvmTarget

// :core is the Kotlin counterpart of the iOS CoffeeGramsCore Swift package.
//
// INVARIANT: this module is plain Kotlin/JVM and must NEVER depend on Android.
// It applies the kotlin-jvm plugin (not kotlin-android) precisely so that an
// accidental `import android.*` fails to compile rather than slipping through.
// It must stay runnable headless via `./gradlew :core:test` — no emulator, no device.
plugins {
    alias(libs.plugins.kotlin.jvm)
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
        // The pure-logic module holds the correctness proof for the whole port,
        // so it is held to warnings-as-errors in every build type, not just release.
        allWarningsAsErrors.set(true)
    }
}

dependencies {
    implementation(libs.kotlinx.coroutines.core)

    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.kotlin.test)
    testImplementation(libs.kotlinx.coroutines.test)
    testRuntimeOnly(libs.junit.platform.launcher)
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
    testLogging {
        events("passed", "skipped", "failed")
    }

    // False-green protection is Gradle's, not ours: `failOnNoDiscoveredTests`
    // defaults to true in Gradle 9, so if test sources exist but the JUnit
    // Platform discovers nothing — the failure mode if kotlin-test ever stopped
    // resolving to its kotlin-test-junit5 variant — the task fails rather than
    // reporting a green build that proved nothing. Verified by removing a @Test
    // annotation and confirming the build fails. Do not set it to false.
}
