import org.jetbrains.kotlin.gradle.dsl.JvmTarget

// AGP 9 applies Kotlin itself — the separate `org.jetbrains.kotlin.android`
// plugin is no longer required (and is rejected if applied).
// See https://kotl.in/gradle/agp-built-in-kotlin
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
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

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)

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
    androidTestImplementation(libs.androidx.test.ext.junit)
    debugImplementation(libs.compose.ui.test.manifest)
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()

    // See :core — Gradle 9's `failOnNoDiscoveredTests` covers the false-green
    // case. ModuleWiringTest exists so this task has sources to discover, which
    // is what arms that protection.
}
