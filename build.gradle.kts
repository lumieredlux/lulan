plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.hilt) apply false
    alias(libs.plugins.detekt)
    alias(libs.plugins.ktlint)
}

detekt {
    config.setFrom(files("config/detekt/detekt.yml"))
    buildUponDefaultConfig = true
    allRules = false
}

tasks.register("ciCheck") {
    dependsOn("detekt")
}

ktlint {
    android.set(true)
    ignoreFailures.set(false)
    outputToConsole.set(true)
}
