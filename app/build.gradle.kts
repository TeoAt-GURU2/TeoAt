import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    id("com.google.gms.google-services")
    id("com.google.android.libraries.mapsplatform.secrets-gradle-plugin")
}

android {
    namespace = "com.example.teoat"
    compileSdk {
        version = release(36)
    }

    signingConfigs {
        getByName("debug") {
            storeFile = file("debug.keystore")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
    }

    val localProperties = Properties()
    val localPropertiesFile = rootProject.file("local.properties")
    if (localPropertiesFile.exists()) {
        localProperties.load(localPropertiesFile.inputStream())
    }

    defaultConfig {
        applicationId = "com.example.teoat"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // 캘린더 API 키 변수 생성
        buildConfigField("String", "GCAL_API_KEY", "\"${localProperties["GCAL_API_KEY"] ?: ""}\"")
        buildConfigField("String", "GCAL_CALENDAR_ID", "\"${localProperties["GCAL_CALENDAR_ID"] ?: ""}\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        getByName("debug") {
            signingConfig = signingConfigs.getByName("debug")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
        isCoreLibraryDesugaringEnabled = true // 캘린더 날짜 지원 위해 유지
    }
    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        viewBinding = true
        buildConfig = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    implementation("androidx.recyclerview:recyclerview:1.3.2")

    // 로그인, 챗봇용 Firebase
    implementation(platform("com.google.firebase:firebase-bom:34.8.0"))
    implementation("com.google.firebase:firebase-analytics")

    // 챗봇 용 Firebase AI
    implementation("com.google.firebase:firebase-ai")

    // 로그인, 회원가입용 Firebase Authentication
    implementation("com.google.firebase:firebase-auth")

    // DB용 Cloud Firebase
    implementation("com.google.firebase:firebase-firestore:26.1.0")

    // ViewModel 및 Coroutine
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.10.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.10.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")

    // 메인 액티비티 내 이벤트 홍보 배너용
    implementation("androidx.viewpager2:viewpager2:1.1.0")
    implementation("com.google.android.material:material:1.13.0")
    // 이미지 로딩을 위한 라이브러리
    implementation("com.github.bumptech.glide:glide:4.9.0")

    // 구글 지도
    implementation("com.google.android.material:material:1.12.0")
    implementation("com.google.android.gms:play-services-maps:18.2.0")
    implementation("com.google.android.gms:play-services-location:21.0.1")

    // 가맹점, 복지시설 조회 API 불러올 때
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-gson:2.11.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")

    // 알림 전송 관련 WorkManager <- 백그라운드 작업
    implementation("androidx.work:work-runtime-ktx:2.11.0")

    // 캘린더 관련
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.0.4")
    implementation("com.github.prolificinteractive:material-calendarview:2.0.1") {
        exclude(group = "com.android.support")
    }
    implementation("com.google.android.gms:play-services-auth:21.2.0")
}