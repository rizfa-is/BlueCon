import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidMultiplatformLibrary)
    `maven-publish`
}

group = "com.siosin.bluecon"
version = "0.1.0"

kotlin {
    // Public contracts and algorithms are portable; only Android has a BLE backend.
    android {
        namespace = "com.siosin.bluecon.bluetooth"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()
        compilerOptions { jvmTarget = JvmTarget.JVM_11 }
        withHostTest { }
    }
    // Metadata targets keep :shared's existing iOS sources consumable. No iOS BLE backend.
    iosArm64()
    iosSimulatorArm64()

    sourceSets {
        commonMain.dependencies { api(libs.coroutines.core) }
        androidMain.dependencies { implementation(libs.coroutines.android) }
        commonTest.dependencies { implementation(libs.kotlin.test) }
    }
}
