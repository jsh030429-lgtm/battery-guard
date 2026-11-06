plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "app.batteryguard"
    compileSdk = 34

    defaultConfig {
        applicationId = "app.batteryguard"
        minSdk = 28
        targetSdk = 34
        versionCode = 1
        versionName = "0.1.0-prototype"

        // 에뮬레이터(x86_64)와 실제 차량(arm64)용 네이티브 라이브러리만 포함
        ndk { abiFilters += listOf("x86_64", "arm64-v8a") }
    }

    buildTypes {
        release {
            // GitHub 웹 업로드 한도(25MB)에 맞추려고 쓰지 않는 코드를 걷어냄. Pleos SDK 코드는 proguard-rules.pro에서 보존
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            // 공모전 제출용 APK는 디버그 키로 서명 (별도 키 관리 없이 설치 가능)
            signingConfig = signingConfigs.getByName("debug")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation("ai.pleos.playground:Vehicle:2.3.2")
    implementation("ai.pleos.playground:TextToSpeech:3.3.1")

    implementation("androidx.activity:activity-compose:1.9.0")
    implementation(platform("androidx.compose:compose-bom:2024.04.01"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")
}
