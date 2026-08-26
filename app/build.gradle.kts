import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.io.FileInputStream
import java.util.Properties

// AGP 9 applies Kotlin itself — the separate `org.jetbrains.kotlin.android`
// plugin is no longer required (and is rejected if applied).
// See https://kotl.in/gradle/agp-built-in-kotlin
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
}

// M12 release signing. keystore.properties is git-ignored and machine-local
// (see keystore.properties.example for the expected shape) — CI has no such
// file and never will, so every use of it below is guarded by .exists().
// Without the guard, CI's `:app:assembleRelease` (see ci.yml) would break
// the moment this config landed; with it, CI keeps producing the same
// unsigned release build it always has; a real signed release becomes
// possible only once a developer creates the file locally.
val keystorePropertiesFile = rootProject.file("keystore.properties")
val keystoreProperties = Properties().apply {
    if (keystorePropertiesFile.exists()) load(FileInputStream(keystorePropertiesFile))
}

android {
    namespace = "com.jrlabapps.coffeegrams"
    compileSdk = libs.versions.compileSdk.get().toInt()
    compileSdkMinor = libs.versions.compileSdkMinor.get().toInt()

    defaultConfig {
        applicationId = "com.jrlabapps.coffeegrams"
        minSdk = libs.versions.minSdk.get().toInt()
        targetSdk = libs.versions.targetSdk.get().toInt()
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        if (keystorePropertiesFile.exists()) {
            create("release") {
                storeFile = file(keystoreProperties["storeFile"] as String)
                storePassword = keystoreProperties["storePassword"] as String
                keyAlias = keystoreProperties["keyAlias"] as String
                keyPassword = keystoreProperties["keyPassword"] as String
            }
        }
    }

    buildTypes {
        debug {
            isMinifyEnabled = false
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            if (keystorePropertiesFile.exists()) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }

    buildFeatures {
        compose = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

// Room schema export: BrewLogDatabase declares exportSchema = true (no
// migration history yet, but the first migration needs a committed v1
// baseline to diff against rather than reconstructing it from memory).
ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
        // Warnings-as-errors on release only, per the repo standards: day-to-day
        // debug builds stay workable, but nothing ships with a warning in it.
        allWarningsAsErrors.set(
            providers.gradleProperty("coffeegrams.warningsAsErrors").map { it.toBoolean() }.orElse(false),
        )
    }
}

// Release compilation is the gate — force warnings-as-errors on those tasks
// regardless of the property above.
tasks.matching { it.name.contains("Release") }.configureEach {
    if (this is org.jetbrains.kotlin.gradle.tasks.KotlinCompile) {
        compilerOptions.allWarningsAsErrors.set(true)
    }
}

dependencies {
    implementation(project(":core"))

    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.billing.ktx)

    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons.extended)
    implementation(libs.compose.ui.tooling.preview)
    debugImplementation(libs.compose.ui.tooling)

    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
    // Not libs.kotlin.test — see the catalog comment. AGP's built-in Kotlin does
    // not auto-select the JUnit 5 variant, so ask for it by name.
    testImplementation(libs.kotlin.test.junit5)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)
    testRuntimeOnly(libs.junit.platform.launcher)

    androidTestImplementation(platform(libs.compose.bom))
    androidTestImplementation(libs.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.test.rules)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.kotlinx.coroutines.test)
    // M10 screenshot harness — full-device screenshots (status bar included),
    // which Compose's own captureToImage() can't do since it only sees the
    // Compose content, not the system chrome around it.
    androidTestImplementation(libs.androidx.test.uiautomator)
    debugImplementation(libs.compose.ui.test.manifest)
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()

    // See :core — Gradle 9's `failOnNoDiscoveredTests` covers the false-green
    // case. ModuleWiringTest exists so this task has sources to discover, which
    // is what arms that protection.
}
