import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.application)
    // AGP 9 起 Kotlin 支持已内置，无需再应用 kotlin-android 插件，Compose 编译器插件仍需单独应用
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.ddw.dcomposeui"
    compileSdk {
        version = release(libs.versions.compileSdk.get().toInt())
    }

    defaultConfig {
        applicationId = "com.ddw.dcomposeui"
        minSdk = libs.versions.minSdk.get().toInt()
        targetSdk = libs.versions.targetSdk.get().toInt()
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            optimization {
                enable = false
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        // 启用 Compose 构建特性
        compose = true
    }
}

// Kotlin 编译配置（AGP 内置 Kotlin 注册的 kotlin 扩展）
kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_11
    }
}

dependencies {
    // Compose BOM，统一管理 Compose 各组件版本
    val composeBom = platform(libs.compose.bom)
    implementation(composeBom)
    androidTestImplementation(composeBom)
    // Compose 套件（UI/图形/预览/Material3/图标/Activity/ViewModel）
    implementation(libs.bundles.compose)
    // Compose 调试工具（Preview 渲染，仅 debug）
    debugImplementation(libs.androidx.compose.ui.tooling)
    // Coil 图片加载（Compose + 基于 OkHttp 的网络加载器）
    implementation(libs.bundles.coil)

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
