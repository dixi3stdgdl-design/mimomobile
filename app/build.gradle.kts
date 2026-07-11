import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
    id("jacoco")
}

jacoco {
    toolVersion = "0.8.12"
}

val keyProperties = Properties().apply {
    val file = rootProject.file("app/key.properties")
    if (file.exists()) load(file.inputStream())
}

android {
    namespace = "com.mimo.mobile"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.mimo.mobile"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    flavorDimensions += "channel"
    productFlavors {
        create("free") {
            dimension = "channel"
            applicationIdSuffix = ".free"
            versionNameSuffix = "-free"
            buildConfigField("String", "TIER", "\"free\"")
        }
        create("pro") {
            dimension = "channel"
            applicationIdSuffix = ".pro"
            versionNameSuffix = "-pro"
            buildConfigField("String", "TIER", "\"pro\"")
        }
    }

    signingConfigs {
        create("release") {
            storeFile = rootProject.file("app/${keyProperties.getProperty("storeFile", "release-keystore.jks")}")
            storePassword = keyProperties.getProperty("storePassword", "")
            keyAlias = keyProperties.getProperty("keyAlias", "")
            keyPassword = keyProperties.getProperty("keyPassword", "")
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("release")
        }
        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2026.02.01"))
    implementation("androidx.core:core-ktx:1.10.1")
    implementation("androidx.core:core-splashscreen:1.0.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.6.1")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.6.1")
    implementation("androidx.activity:activity-compose:1.8.0")
    implementation("androidx.datastore:datastore-preferences:1.2.1")

    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material3:material3-window-size-class:1.3.1")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.animation:animation")
    implementation("androidx.compose.foundation:foundation")

    implementation("androidx.navigation:navigation-compose:2.7.7")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")

    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")

    // Unit tests
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
    testImplementation("org.json:json:20231013")

    // Instrumented tests
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test:runner:1.5.2")
    androidTestImplementation("androidx.test:rules:1.5.0")
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    androidTestImplementation("androidx.compose.material3:material3")
    androidTestImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
}

tasks.register<JacocoReport>("jacocoFullCoverageReport") {
    dependsOn("testFreeDebugUnitTest", "testProDebugUnitTest")

    additionalSourceDirs.setFrom(fileTree("src/main/java") {
        include("**/*.kt")
    })
    sourceDirectories.setFrom(fileTree("src/main/java") {
        include("**/*.kt")
    })
    classDirectories.setFrom(fileTree("build/tmp/kotlin-classes/freeDebug") {
        include("**/*.class")
        exclude("**/R.class", "**/R\$*.class", "**/BuildConfig.*", "**/Manifest*.*")
    })
    executionData.setFrom(fileTree("build") {
        include("**/*.exec", "**/*.ec")
    })

    reports {
        xml.required.set(true)
        html.required.set(true)
        csv.required.set(false)
        xml.outputLocation.set(layout.buildDirectory.file("reports/jacoco/jacocoFullCoverageReport.xml"))
        html.outputLocation.set(layout.buildDirectory.dir("reports/jacoco/jacocoFullCoverageReport"))
    }
}

tasks.register<JacocoReport>("jacocoUnitTestReport") {
    dependsOn("testFreeDebugUnitTest")

    additionalSourceDirs.setFrom(fileTree("src/main/java") {
        include("**/*.kt")
    })
    sourceDirectories.setFrom(fileTree("src/main/java") {
        include("**/*.kt")
    })
    classDirectories.setFrom(fileTree("build/tmp/kotlin-classes/freeDebug") {
        include("**/*.class")
        exclude("**/R.class", "**/R\$*.class", "**/BuildConfig.*", "**/Manifest*.*")
    })
    executionData.setFrom(fileTree("build") {
        include("**/*.exec", "**/*.ec")
    })

    reports {
        xml.required.set(true)
        html.required.set(true)
        csv.required.set(false)
        xml.outputLocation.set(layout.buildDirectory.file("reports/jacoco/jacocoUnitTestReport.xml"))
        html.outputLocation.set(layout.buildDirectory.dir("reports/jacoco/jacocoUnitTestReport"))
    }
}
