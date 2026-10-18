// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.hilt) apply false
    alias(libs.plugins.room) apply false
    alias(libs.plugins.spotless)
}

// Formatting: ./gradlew spotlessApply (ktlint + Compose rules; settings in .editorconfig).
spotless {
    val ktlintVersion = libs.versions.ktlint.get()
    val composeRules = "io.nlopez.compose.rules:ktlint:${libs.versions.composeRules.get()}"
    kotlin {
        target("app/src/**/*.kt")
        ktlint(ktlintVersion)
            .customRuleSets(listOf(composeRules))
            // LocalDisplay carries the chosen script and menu language to every screen.
            .editorConfigOverride(mapOf("compose_allowed_composition_locals" to "LocalDisplay"))
    }
    kotlinGradle {
        target("*.gradle.kts", "app/*.gradle.kts")
        ktlint(ktlintVersion)
    }
}
