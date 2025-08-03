// Top-level build.gradle
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    id("com.google.gms.google-services") version "4.4.3" apply false
}

// Ensure repositories are defined for the buildscript
buildscript {
    repositories {
        google() // Required for google-services plugin
        mavenCentral()
    }
}