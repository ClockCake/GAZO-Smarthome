
val bizBomVersion: String by project
val sdkVersion: String by project

plugins {
    id("com.android.application")
    id("kotlin-android")
    // The Flutter Gradle Plugin must be applied after the Android and Kotlin Gradle plugins.
    id("dev.flutter.flutter-gradle-plugin")

}


kotlin {
    jvmToolchain(17) // 指定使用 JDK 17 来编译 Kotlin 代码
}
// Kotlin DSL
java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}
android {
    namespace = "com.jiyoujiaju.jijiahui"
    compileSdk = flutter.compileSdkVersion
    ndkVersion = "27.0.12077973"

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = JavaVersion.VERSION_17.toString()
    }

    defaultConfig {
        // TODO: Specify your own unique Application ID (https://developer.android.com/studio/build/application-id.html).
        applicationId = "com.jiyoujiaju.jijiahui"
        minSdk =  23   // flutter.minSdkVersion
        targetSdk = 34 //flutter.targetSdkVersion
        versionCode = flutter.versionCode
        versionName = flutter.versionName
        multiDexEnabled = true
        ndk {
            abiFilters += "armeabi-v7a"
            abiFilters += "arm64-v8a"
        }
    }
    packaging {
        // .so 冲突时只取第一个
        jniLibs {
            pickFirsts += listOf(
                "lib/*/libc++_shared.so",
                "lib/x86/libc++_shared.so",
                "lib/x86_64/libc++_shared.so",
                "lib/armeabi-v7a/libc++_shared.so",
                "lib/arm64-v8a/libc++_shared.so",
                ////涂鸦官当的配置
                "lib/*/liblog.so",
                "lib/*/libyuv.so",
                "lib/*/libopenh264.so",
                "lib/*/libv8wrapper.so",
                "lib/*/libv8android.so"
            )
        }
        // 资源排除配置
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    signingConfigs {
        create("platform") {
            storeFile = file("keystore/jijiahui")
            storePassword = "940824"
            keyAlias = "JiJiaHui"
            keyPassword = "940824"
            storeType = "PKCS12"
        }
    }

    buildTypes {
        val signConfig = signingConfigs.getByName("platform")
        getByName("release") {
            isMinifyEnabled = false
            isShrinkResources = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro"
            )
            signingConfig = signConfig
        }
        getByName("debug") {
            isMinifyEnabled = false
            isShrinkResources = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro"

            )
            signingConfig = signConfig
        }
    }


}

configurations.all {
    exclude(group = "com.thingclips.smart", module = "thingsmart-modularCampAnno")
}

flutter {
    source = "../.."
}
configurations.all {
    // 把所有传递进来的 commons-io:commons-io 排除掉
    exclude(group = "commons-io", module = "commons-io")
}
dependencies {
    // 第三方库依赖
    implementation("com.google.code.gson:gson:2.10.1")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.compose.ui:ui-text-android:1.8.1")
    implementation("com.alibaba:fastjson:1.1.67.android")
    implementation("com.squareup.okhttp3:okhttp-urlconnection:3.14.9")
    implementation("com.facebook.soloader:soloader:0.10.4")

    implementation("com.thingclips.smart:thingsmart:$sdkVersion")
    // 强制平台依赖
    api(enforcedPlatform("com.thingclips.smart:thingsmart-BizBundlesBom:$bizBomVersion"))

    // 家庭业务包
    api("com.thingclips.smart:thingsmart-bizbundle-family")

    // 配网业务包
    api("com.thingclips.smart:thingsmart-bizbundle-device_activator")
    // 若需要使用扫一扫功能，则需要依赖扫码业务包
    api("com.thingclips.smart:thingsmart-bizbundle-qrcode_mlkit")
    //设备详情
    implementation("com.thingclips.smart:thingsmart-bizbundle-panelmore")

    // 设备控制业务包 - 必选
    implementation("com.thingclips.smart:thingsmart-bizbundle-panel"){
        exclude(group = "com.tencent.mm.opensdk", module = "wechat-sdk-android-without-mta")

    }
    // 基础扩展能力 - 必选
    implementation("com.thingclips.smart:thingsmart-bizbundle-basekit"){
        exclude(group = "com.tencent.mm.opensdk", module = "wechat-sdk-android-without-mta")

    }
     // 业务扩展能力 - 必选
    implementation("com.thingclips.smart:thingsmart-bizbundle-bizkit"){
        exclude(group = "com.tencent.mm.opensdk", module = "wechat-sdk-android-without-mta")

    }
    // 设备控制相关能力 - 必选
    implementation("com.thingclips.smart:thingsmart-bizbundle-devicekit"){
        exclude(group = "com.tencent.mm.opensdk", module = "wechat-sdk-android-without-mta")

    }
    implementation("com.thingclips.smart:thingsmart-bizbundle-mediakit")
    implementation("com.thingclips.smart:thingsmart-ipcsdk:6.4.2")

    // 本地 AAR 文件
    implementation(
        fileTree(
            mapOf(
                "dir" to "libs",
                "include" to listOf("*.aar")
            )
        )
    )

}

