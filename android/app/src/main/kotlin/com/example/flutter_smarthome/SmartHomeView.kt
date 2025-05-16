package com.jiyoujiaju.jijiahui // <-- 确保这是你正确的包名

import android.content.Context
import android.graphics.Color
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater // 用于加载 XML 布局
import android.widget.Button
import android.widget.FrameLayout // 你可以选择 FrameLayout, LinearLayout, RelativeLayout 等作为基类
import android.widget.TextView
// 导入你需要的其他 Android UI 组件

// SmartHomeView 就是你的原生 Android 页面/视图组件
class SmartHomeView(
    context: Context,
    private val creationParams: Map<String?, Any?>? // 从 Flutter 传递过来的参数
) : FrameLayout(context) { // 你可以继承自任何合适的 ViewGroup，例如 LinearLayout, RelativeLayout

    private val TAG = "SmartHomeView"

    init {
        Log.d(TAG, "SmartHomeView 初始化开始，接收到的参数: $creationParams")

        // 你可以在这里使用从 Flutter 传递过来的 creationParams
        val initialWidth = creationParams?.get("initialWidth") as? Double
        val initialHeight = creationParams?.get("initialHeight") as? Double
        val tabBarHeight = creationParams?.get("tabBarHeight") as? Double // 这个参数来自你的 Flutter 代码

        Log.d(TAG, "从 Flutter 接收到的参数: initialWidth=$initialWidth, initialHeight=$initialHeight, tabBarHeight=$tabBarHeight")
        Log.d(TAG, "当前时间（示例参数使用）: ${System.currentTimeMillis()}")


        // --------------------------------------------------------------------
        // 在这里构建你的原生 Android UI 界面
        // 你可以通过代码动态创建视图，或者加载一个 XML 布局文件
        // --------------------------------------------------------------------

        // --- 示例 1：通过代码动态创建一个简单的 TextView 和 Button ---
        val textView = TextView(context).apply {
            text = "这是安卓原生智能家居页面\n(由 SmartHomeView.kt 驱动)\n接收到的 tabBarHeight: $tabBarHeight"
            textSize = 16f
            setTextColor(Color.DKGRAY)
            gravity = Gravity.CENTER
            setPadding(20, 20, 20, 20)
        }
        this.addView(textView, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT).apply {
            gravity = Gravity.CENTER
        })

        val nativeButton = Button(context).apply {
            text = "这是一个原生安卓按钮"
            setOnClickListener {
                Log.d(TAG, "原生安卓按钮被点击了！")
                // 如果需要，你可以在这里通过 MethodChannel 与 Flutter 通信
                // 例如：告诉 Flutter 某个操作已完成
                textView.text = "原生按钮被点击了！时间: ${System.currentTimeMillis()}"
            }
        }
        val buttonParams = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT).apply {
            gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
            bottomMargin = 150 // 距离底部 150px
        }
        this.addView(nativeButton, buttonParams)

        // --- 示例 2：加载一个 XML 布局文件 (推荐用于复杂界面) ---
        /*
        // 1. 在 res/layout 目录下创建一个 XML 文件，例如 `my_smart_home_layout.xml`
        // <LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
        //     android:layout_width="match_parent"
        //     android:layout_height="match_parent"
        //     android:orientation="vertical"
        //     android:gravity="center">
        //     <TextView
        //         android:id="@+id/native_text_view_from_xml"
        //         android:layout_width="wrap_content"
        //         android:layout_height="wrap_content"
        //         android:text="来自 XML 布局的原生文本"/>
        //     <Button
        //         android:id="@+id/native_button_from_xml"
        //         android:layout_width="wrap_content"
        //         android:layout_height="wrap_content"
        //         android:text="XML 中的按钮"/>
        // </LinearLayout>

        // 2. 在代码中加载它：
        // val inflater = LayoutInflater.from(context)
        // val rootView = inflater.inflate(R.layout.my_smart_home_layout, this, true) // 'this' 是 FrameLayout, true 表示附加到 this

        // 3. 获取并操作 XML 中的视图：
        // val textViewFromXml = rootView.findViewById<TextView>(R.id.native_text_view_from_xml)
        // val buttonFromXml = rootView.findViewById<Button>(R.id.native_button_from_xml)
        // textViewFromXml.text = "通过代码更新的 XML 文本内容。tabBarHeight: $tabBarHeight"
        // buttonFromXml.setOnClickListener {
        //     Log.d(TAG, "XML 布局中的按钮被点击！")
        //     textViewFromXml.text = "XML 按钮被点击了！"
        // }
        */

        // 设置一个背景色方便区分
        this.setBackgroundColor(Color.parseColor("#E0E0E0")) // 浅灰色背景

        Log.d(TAG, "SmartHomeView UI 构建完成。")
    }

    // 这个方法会在 PlatformView 被销毁时调用 (由 NativeView.dispose() 触发)
    // 在这里释放所有持有的原生资源
    fun cleanup() {
        Log.d(TAG, "SmartHomeView.cleanup() 被调用。在这里释放原生资源。")
        // 例如：
        // - 注销广播接收器
        // - 停止动画、线程
        // - 清理图片缓存
        // - 释放摄像头、传感器等硬件资源
    }
}