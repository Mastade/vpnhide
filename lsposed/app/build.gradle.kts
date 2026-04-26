import java.io.FileInputStream
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.atomicfu)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.gobley.cargo)
    alias(libs.plugins.gobley.uniffi)
}

cargo {
    packageDirectory = layout.projectDirectory.dir("../native")
    // Don't bundle the Rust lib into Android unit-test resources. Unit tests
    // never load the native lib, and bundling drags in a Linux x64 cargo
    // build that fails because the source uses Android-shaped ioctl request
    // types incompatible with glibc.
    builds.withType(gobley.gradle.cargo.dsl.CargoJvmBuild::class.java).configureEach {
        androidUnitTest.set(false)
    }
}

uniffi {
    generateFromLibrary {
        packageName = "dev.okhsunrog.vpnhide.checks"
    }
}

android {
    namespace = "dev.okhsunrog.vpnhide"
    compileSdk = 35

    // Effective build version from ../scripts/build-version.py:
    //   release tag    -> "0.6.2"
    //   dev build      -> "0.6.1-5-gabc1234" (+"-dirty" if uncommitted)
    //   no git         -> VERSION file
    // Python instead of bash so Windows contributors can build without WSL.
    // Script is stdlib-only — no `uv` / pip install needed. `python` on
    // Windows, `python3` elsewhere: Ubuntu 22.04+ ships only the latter,
    // Windows python.org / Store installer ships only the former.
    val isWindows = System.getProperty("os.name").lowercase().contains("windows")
    val pythonExe = if (isWindows) "python" else "python3"
    val buildVersion: String =
        providers
            .exec {
                commandLine(
                    pythonExe,
                    rootProject.projectDir.parentFile.resolve("scripts/build-version.py").absolutePath,
                )
            }.standardOutput.asText
            .get()
            .trim()

    defaultConfig {
        applicationId = "dev.okhsunrog.vpnhide"
        minSdk = 29
        targetSdk = 35
        versionCode = 701
        versionName = buildVersion

        ndk {
            abiFilters += listOf("arm64-v8a")
        }
    }

    signingConfigs {
        create("release") {
            val keystorePropertiesFile = rootProject.file("keystore.properties")
            if (keystorePropertiesFile.exists()) {
                val keystoreProperties = Properties()
                keystoreProperties.load(FileInputStream(keystorePropertiesFile))
                keyAlias = keystoreProperties["keyAlias"] as String
                keyPassword = keystoreProperties["password"] as String
                storeFile = file(keystoreProperties["storeFile"] as String)
                storePassword = keystoreProperties["password"] as String
            }
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    buildTypes {
        release {
            signingConfig = signingConfigs.getByName("release")
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
        debug {
            signingConfig = signingConfigs.getByName("release")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    packaging {
        resources.excludes += "META-INF/*.kotlin_module"
    }
}

dependencies {
    // Xposed API — compileOnly so it's not bundled into the APK.
    compileOnly("de.robv.android.xposed:api:82")

    // Android 12 SplashScreen API, backported to API 23+.
    implementation("androidx.core:core-splashscreen:1.0.1")

    // Compose UI
    implementation(libs.core.ktx)
    implementation(libs.activity.compose)
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.material3)
    implementation("androidx.compose.material:material-icons-extended")
    implementation("io.github.oikvpqya.compose.fastscroller:fastscroller-material3:0.3.2")
    implementation("io.github.oikvpqya.compose.fastscroller:fastscroller-indicator:0.3.2")
    implementation(libs.compose.ui.tooling.preview)
    debugImplementation(libs.compose.ui.tooling)

    testImplementation("junit:junit:4.13.2")
}
