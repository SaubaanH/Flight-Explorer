import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    kotlin("multiplatform")
    kotlin("plugin.serialization")
    id("com.android.kotlin.multiplatform.library")
    id("app.cash.sqldelight")
}

val ktorVersion = "3.1.3"
val coroutinesVersion = "1.10.2"
val sqlDelightVersion = "2.1.0"
val serializationVersion = "1.8.1"

kotlin {
    android {
        namespace = "travel.roame.takehome.shared"
        compileSdk { version = release(37) { minorApiLevel = 0 } }
        minSdk = 23
        compilerOptions { jvmTarget.set(JvmTarget.JVM_17) }
    }
    jvm()
    listOf(iosArm64(), iosSimulatorArm64(), iosX64()).forEach {
        it.binaries.framework { baseName = "Shared"; isStatic = true }
    }

    sourceSets {
        commonMain.dependencies {
            implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$coroutinesVersion")
            implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:$serializationVersion")
            implementation("io.ktor:ktor-client-core:$ktorVersion")
            implementation("io.ktor:ktor-client-content-negotiation:$ktorVersion")
            implementation("io.ktor:ktor-serialization-kotlinx-json:$ktorVersion")
            implementation("app.cash.sqldelight:runtime:$sqlDelightVersion")
            implementation("app.cash.sqldelight:coroutines-extensions:$sqlDelightVersion")
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:$coroutinesVersion")
        }
        val jvmTest by getting {
            dependencies {
                implementation(kotlin("test"))
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:$coroutinesVersion")
                implementation("io.ktor:ktor-client-mock:$ktorVersion")
                implementation("app.cash.sqldelight:sqlite-driver:$sqlDelightVersion")
            }
        }
        androidMain.dependencies {
            implementation("io.ktor:ktor-client-okhttp:$ktorVersion")
            implementation("app.cash.sqldelight:android-driver:$sqlDelightVersion")
        }
        iosMain.dependencies {
            implementation("io.ktor:ktor-client-darwin:$ktorVersion")
            implementation("app.cash.sqldelight:native-driver:$sqlDelightVersion")
        }
        jvmMain.dependencies {
            implementation("io.ktor:ktor-client-cio:$ktorVersion")
            implementation("app.cash.sqldelight:sqlite-driver:$sqlDelightVersion")
        }
    }
}

sqldelight {
    databases {
        create("FlightDatabase") {
            packageName.set("travel.roame.takehome.db")
        }
    }
}
