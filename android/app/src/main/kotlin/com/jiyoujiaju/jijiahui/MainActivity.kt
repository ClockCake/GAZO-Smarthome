package com.jiyoujiaju.jijiahui

import android.content.Intent
import com.thingclips.smart.activator.plug.mesosphere.ThingDeviceActivatorManager
import com.thingclips.smart.bizbundle.initializer.BizBundleInitializer
import io.flutter.embedding.android.FlutterActivity
import io.flutter.embedding.engine.FlutterEngine

/**
 * MainActivity：初始化 Flutter 引擎并手动注册本地插件
 */
class MainActivity : FlutterActivity() {

    override fun configureFlutterEngine(flutterEngine: FlutterEngine) {
        super.configureFlutterEngine(flutterEngine)
        // 手动把你的 FlutterUserPlugin 注册进来
        flutterEngine
            .plugins
            .add(FlutterUserPlugin())

        // 把 factory 注册到 engine，让它知道 viewType 对应哪个原生组件
        flutterEngine
            .platformViewsController
            .registry
            .registerViewFactory(
                "native_android_smartlife",
                SmartHomeViewFactory()
            )
    }
    override fun onResume() {
        super.onResume()
        // 每次 Activity 可见／从后台恢复时都会执行
        BizBundleInitializer.onLogin()
    }

}