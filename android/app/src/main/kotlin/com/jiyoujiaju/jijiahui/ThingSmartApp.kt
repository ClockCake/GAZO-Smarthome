package com.jiyoujiaju.jijiahui

import android.app.Application
import android.util.Log
import com.thingclips.smart.api.MicroContext
import com.thingclips.smart.api.router.UrlBuilder
import com.thingclips.smart.api.service.RedirectService
import com.thingclips.smart.api.service.RouteEventListener
import com.thingclips.smart.api.service.ServiceEventListener
import com.thingclips.smart.bizbundle.initializer.BizBundleInitializer
import com.thingclips.smart.commonbiz.bizbundle.family.api.AbsBizBundleFamilyService
import com.thingclips.smart.home.sdk.ThingHomeSdk

//@HiltAndroidApp
class ThingSmartApp : Application() {
    override fun onCreate() {
        super.onCreate()

        // 启动 IoT SDK
        ThingHomeSdk.init(this)
        ThingHomeSdk.setDebugMode(true);
        // 业务包初始化
        BizBundleInitializer.init(
            this,
            // RouteEventListener：路由未实现回调
            object : RouteEventListener {
                override fun onFaild(errorCode: Int, urlBuilder: UrlBuilder) {
                    Log.e(
                        "router not implement",
                        "${urlBuilder.target}${urlBuilder.params}"
                    )
                }
            },
            // ServiceEventListener：服务未实现回调
            object : ServiceEventListener {
                override fun onFaild(serviceName: String) {
                    Log.e("service not implement", serviceName)
                }
            }
        )

        // 注册家庭服务，商城业务包可以不注册此服务
        BizBundleInitializer.registerService(
            AbsBizBundleFamilyService::class.java,
            BizBundleFamilyServiceImpl()
        )

    }

    override fun onTerminate() {
        super.onTerminate()
        // 仅在模拟器或调试时 onTerminate 才会被调用，生产环境不一定会走
        ThingHomeSdk.onDestroy()
    }
}



