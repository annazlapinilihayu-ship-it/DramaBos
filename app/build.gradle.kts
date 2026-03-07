import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.util.Properties
import java.io.FileInputStream

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
}


val localProps = Properties()
rootProject.file("local.properties").let { f ->
    if (f.exists()) localProps.load(FileInputStream(f))
}

android {
    namespace = "com.sonzaix.shortxrama"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.sonzaix.shortxrama"
        minSdk = 24
        targetSdk = 36
        versionCode = 7
        versionName = "X2"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }

        ndk {
            abiFilters += listOf("arm64-v8a", "armeabi-v7a")
        }

        buildConfigField("String", "API_BASE_URL", "\"https://api.sonzaix.indevs.in/\"")
    }

    signingConfigs {
        create("release") {
            val keyStorePath = System.getenv("SIGNING_KEY_STORE_PATH")
            storeFile = if (keyStorePath != null) file(keyStorePath) else file("keystore.jks")

            storePassword = System.getenv("SIGNING_STORE_PASSWORD")
                ?: localProps.getProperty("signing.storePassword")
            keyAlias = System.getenv("SIGNING_KEY_ALIAS")
                ?: localProps.getProperty("signing.keyAlias")
            keyPassword = System.getenv("SIGNING_KEY_PASSWORD")
                ?: localProps.getProperty("signing.keyPassword")
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
            
            ndk {
                debugSymbolLevel = "full"
            }
        }
        
        debug {
            isMinifyEnabled = false 
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        viewBinding = true
        buildConfig = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
        // Prevent stripping of specific native libraries
        jniLibs {
            excludes.add("lib/*/libandroidx.graphics.path.so")
            excludes.add("lib/*/libdatastore_shared_counter.so")
        }
    }

}

dependencies {
    implementation("androidx.core:core-ktx:1.17.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.10.0")
    implementation("androidx.activity:activity-compose:1.12.2")
    implementation(platform("androidx.compose:compose-bom:2026.01.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material:material")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended:1.7.8")
    implementation("androidx.navigation:navigation-compose:2.9.6")
    implementation("androidx.work:work-runtime-ktx:2.10.0")
    implementation("com.squareup.retrofit2:retrofit:3.0.0")
    implementation("com.squareup.retrofit2:converter-gson:3.0.0")
    implementation("com.squareup.okhttp3:logging-interceptor:5.3.2")
    implementation("io.coil-kt:coil-compose:2.7.0")
    implementation("androidx.media3:media3-exoplayer:1.9.0")
    implementation("androidx.media3:media3-exoplayer-hls:1.9.0")
    implementation("androidx.media3:media3-ui:1.9.0")
    implementation("androidx.media3:media3-common:1.9.0")
    implementation("androidx.media3:media3-datasource-okhttp:1.9.0")
    implementation("androidx.datastore:datastore-preferences:1.2.0")
    implementation("com.google.code.gson:gson:2.13.2")
    implementation("androidx.compose.foundation:foundation:1.10.1")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}
