// Spanda Sync: end-to-end encrypted sync (SPEC.md). Kotlin Multiplatform, no platform dependencies:
// cryptographic primitives are supplied by the app through CryptoPrimitives (SPEC.md §3).
plugins {
    id("org.jetbrains.kotlin.multiplatform") version "2.4.21"
}

group = "com.spanda"
version = "0.1.0-SNAPSHOT"

kotlin {
    jvmToolchain(17)
    jvm()
    iosArm64()
    iosSimulatorArm64()

    sourceSets {
        commonTest.dependencies {
            implementation(kotlin("test"))
        }
    }
}
