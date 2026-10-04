import java.util.Properties

// AGP 9 has built-in Kotlin: do not apply org.jetbrains.kotlin.android here.
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

// Release signing comes from keystore.properties (local, gitignored) or APODROID_* env vars (CI).
// Without either, the release build is left unsigned, which is what F-Droid expects.
val keystoreProps = Properties().apply {
    rootProject.file("keystore.properties").takeIf { it.exists() }?.reader()?.use { load(it) }
}
fun signingValue(prop: String, env: String): String? = keystoreProps.getProperty(prop) ?: System.getenv(env)

android {
    namespace = "io.github.buerlino.apodroid"
    compileSdk = 37

    defaultConfig {
        applicationId = "io.github.buerlino.apodroid"
        minSdk = 29
        targetSdk = 37
        versionCode = 3
        versionName = "0.1.2"
    }

    val releaseKeystore = signingValue("storeFile", "APODROID_KEYSTORE_FILE")
    signingConfigs {
        if (releaseKeystore != null) {
            create("release") {
                storeFile = rootProject.file(releaseKeystore)
                storePassword = signingValue("storePassword", "APODROID_KEYSTORE_PASSWORD")
                keyAlias = signingValue("keyAlias", "APODROID_KEY_ALIAS")
                keyPassword = signingValue("keyPassword", "APODROID_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            // R8 shrinks and optimizes the code (asked for in the F-Droid review). kotlinx.serialization
            // ships its own keep rules, so no proguard-rules.pro is needed.
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"))
            signingConfigs.findByName("release")?.let { signingConfig = it }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures { compose = true }

    // AGP otherwise embeds an encrypted dependency list that only Google Play can read; F-Droid rejects it.
    dependenciesInfo {
        includeInApk = false
        includeInBundle = false
    }
}

dependencies {
    implementation(project(":core"))
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.material3)
    implementation(libs.activity.compose)
    implementation(libs.core.ktx)
}
