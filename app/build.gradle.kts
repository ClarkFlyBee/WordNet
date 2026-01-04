plugins {
    alias(libs.plugins.android.application)
    id("org.jetbrains.kotlin.kapt")
}

android {
    namespace = "com.wcw.wordnet"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.wcw.wordnet"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    buildFeatures {
        viewBinding = true
    }
}

dependencies {

    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)

    val roomVersion = "2.6.1"  // 使用稳定版本
    implementation("androidx.room:room-runtime:$roomVersion")
    annotationProcessor("androidx.room:room-compiler:$roomVersion")
    // ✅ 添加 RxJava2 依赖（用于 DAO 中的 Single）
    implementation("io.reactivex.rxjava2:rxjava:2.2.21")
    // ✅ 添加 Room-RxJava2 适配器
    implementation("androidx.room:room-rxjava2:2.6.1") // 版本号与 room-runtime 保持一致
    // ✅ 添加这一行：RxAndroid
    implementation("io.reactivex.rxjava2:rxandroid:2.1.1")
    // ✅ 降回旧版本
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.5.0")
    // ============ 添加 Navigation 依赖 ============
    implementation("androidx.navigation:navigation-fragment:2.7.7")
    implementation("androidx.navigation:navigation-ui:2.7.7")

}