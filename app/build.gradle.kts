import java.util.Properties
import java.io.ByteArrayOutputStream

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.gms.google-services")
    alias(libs.plugins.kotlin.compose)
}

fun Project.gitCmd(vararg args: String): String? {
    return try {
        // Use Java's ProcessBuilder instead of project.exec
        val process = ProcessBuilder("git", *args)
            .directory(this.rootDir) // Ensure command runs from the project's root directory
            .redirectOutput(ProcessBuilder.Redirect.PIPE)
            .redirectError(ProcessBuilder.Redirect.PIPE)
            .start()

        // Wait for the process to complete, with a timeout
        process.waitFor(5, TimeUnit.SECONDS)

        // Read the output and return it, or null if it's empty
        process.inputStream.bufferedReader().readText().trim().ifEmpty { null }
    } catch (e: Exception) {
        // If git isn't installed or it's not a git repo, return null
        project.logger.warn("Could not run git command: ${e.message}")
        null
    }
}

// ⭐ No changes needed here, they work with the revised gitCmd ⭐
fun Project.gitVersionCode(): Int =
    gitCmd("rev-list", "--count", "HEAD")?.toIntOrNull() ?: 1

fun Project.gitVersionName(): String =
    gitCmd("describe", "--tags", "--dirty", "--always") ?: "0.0.0-dev"

android {
    namespace = "com.proxilocal.hyperlocal"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.proxilocal.hyperlocal"
        minSdk = 24
        targetSdk = 34

        // ⭐ automatic versioning ⭐
        versionCode = gitVersionCode()   // e.g. 153 commits ⇒ 153
        versionName = gitVersionName()   // e.g. v1.2.3‑4‑gabc1234

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildFeatures {
        compose = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.1"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
        isCoreLibraryDesugaringEnabled  = true // Enable desugaring for minSdk 24
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    packaging {
        jniLibs {
            useLegacyPackaging = true
        }
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    signingConfigs {
        create("release") {
            val propFile = project.rootProject.file("local.properties")
            val properties = Properties()
            if (propFile.exists()) {
                properties.load(propFile.inputStream())
            }
            keyAlias = properties.getProperty("MYAPP_RELEASE_KEY_ALIAS")
            keyPassword = properties.getProperty("MYAPP_RELEASE_KEY_PASSWORD")
            storeFile = if (properties.getProperty("MYAPP_RELEASE_STORE_FILE") != null) {
                project.rootProject.file(properties.getProperty("MYAPP_RELEASE_STORE_FILE"))
            } else {
                null
            }
            storePassword = properties.getProperty("MYAPP_RELEASE_STORE_PASSWORD")
        }
    }

    buildTypes {
        getByName("release") {
            signingConfig = signingConfigs.getByName("release")
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    lint {
        baseline = file("lint-baseline.xml")
    }
}

dependencies {
    // Firebase BoM (must be declared first)
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth)
    implementation(libs.firebase.analytics)
    implementation(libs.play.services.auth) // For Google Sign-In

    // Jetpack Compose and other dependencies
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.ui)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.ui.tooling.preview)
    debugImplementation(libs.androidx.ui.tooling)
    implementation(libs.coil.compose)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.accompanist.systemuicontroller)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.core.ktx)
    implementation(libs.mapbox.maps)
    implementation(libs.play.services.location)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.play.billing)

    // Desugaring for minSdk 24
    coreLibraryDesugaring(libs.desugar.jdk.libs)

    // Testing dependencies
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    testImplementation(kotlin("test"))
    testImplementation(libs.mockito.core)
    testImplementation(libs.mockito.inline)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.mockito.kotlin)
}