package com.jiyoujiaju.jijiahui // 请替换为你的实际包名

import DeviceCardAdapter
import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.graphics.Rect
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.util.AttributeSet
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.FrameLayout
import android.widget.HorizontalScrollView
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import com.thingclips.smart.activator.plug.mesosphere.ThingDeviceActivatorManager
import com.thingclips.smart.activator.plug.mesosphere.api.IThingDeviceActiveListener
import com.thingclips.smart.api.MicroContext
import com.thingclips.smart.api.service.MicroServiceManager
import com.thingclips.smart.commonbiz.bizbundle.family.api.AbsBizBundleFamilyService
import com.thingclips.smart.home.sdk.ThingHomeSdk
import com.thingclips.smart.home.sdk.bean.HomeBean
import com.thingclips.smart.home.sdk.bean.RoomBean
import com.thingclips.smart.home.sdk.callback.IThingGetHomeListCallback
import com.thingclips.smart.home.sdk.callback.IThingHomeResultCallback
import com.thingclips.smart.panelcaller.api.AbsPanelCallerService
import com.thingclips.smart.sdk.bean.DeviceBean


class SmartHomeView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
    private val creationParams: Map<String, Any?>? = null
) : FrameLayout(context, attrs, defStyleAttr) {

    private val TAG = "SmartHomeView"

    private val Float.dp: Int
        get() = (this * resources.displayMetrics.density).toInt()

    private lateinit var buttonContainer: LinearLayout
    private lateinit var contentRecyclerView: ExpandedHeightRecyclerView
    private lateinit var deviceCardAdapter: DeviceCardAdapter
    private val buttons = mutableListOf<Button>()
    private var homeId: Long = 0 //homeId
    private var homeName: String? = null //homeName
    private lateinit var homeTitleTextView: TextView  //家庭名
    private var rooms: List<RoomBean> = emptyList()
    // ① 定义一个广播接收器，用来监听登录成功事件
    private val loginReceiver = object : BroadcastReceiver() {
        override fun onReceive(ctx: Context?, intent: Intent?) {
            // 收到登录成功广播后，重新拉取家庭和设备数据
            Log.d(TAG, "收到登录成功广播，开始刷新数据")
            // 这里可以直接调用 initData() 或者 requestTuyaHomeData()，看你想怎样组织
            requestTuyaHomeData()
        }
    }
    init {
        Log.d(TAG, "SmartHomeView 初始化，参数: $creationParams")
        initView()
        initData() // 在这里发起网络请求，加载数据

        // ② 在这里注册广播：监听 action = "loginStatus"
        val filter = IntentFilter("loginStatus")
        // 使用 ApplicationContext（或传进来的 context）动态注册
        ContextCompat.registerReceiver(
            context.applicationContext,
            loginReceiver,
            filter,
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
    }
    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        try {
            // ③ 在 View 从 Window 上移除时注销广播，避免内存泄漏
            context.applicationContext.unregisterReceiver(loginReceiver)
            Log.d(TAG, "SmartHomeView 注销登录广播接收器")
        } catch (e: Exception) {
            Log.w(TAG, "注销 Receiver 失败：${e.message}")
        }
    }
    /** 1. 初始化所有 UI 元素并添加到布局 */
    private fun initView() {
        // 计算高度
        val screenHeight = resources.displayMetrics.heightPixels
        val webViewHeight = screenHeight / 3
        val cardHeight = 80f.dp
        val overlapOffset = cardHeight / 2

        // 外层 ScrollView + 垂直容器
        val scrollView = ScrollView(context).apply {
            layoutParams = LayoutParams(
                LayoutParams.MATCH_PARENT,
                LayoutParams.MATCH_PARENT
            )
            isFillViewport = true
        }
        val container = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LayoutParams(
                LayoutParams.MATCH_PARENT,
                LayoutParams.WRAP_CONTENT
            )
        }

        // --- 1. WebView 部分 ---
        container.addView(CustomWebView(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                webViewHeight
            )
            // 在这里注册浮层按钮点击回调
            setOnOverlayButtonClickListener {
                Log.d(TAG, "浮层按钮被点击，开始设备配网")
                requestTuyaDeviceConfig()
            }
            try { loadHomePage() } catch (e: Exception) { Log.e(TAG, "加载失败", e) }
        })

        // --- 2. 悬浮卡片 + 阴影 ---
        container.addView(createOverlappingCard(cardHeight, overlapOffset))

        // --- 3. 标题行 ---
        container.addView(createTitleRow())

        // --- 4. 横向按钮栏 ---
        buttonContainer = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
        }
        container.addView(HorizontalScrollView(context).apply {
            isHorizontalScrollBarEnabled = false
            addView(buttonContainer)
        })

        deviceCardAdapter = DeviceCardAdapter(context, emptyList()).apply {
            onItemClick = { device, pos ->
                Log.d(TAG, "点击了设备：${device.name} ${device.devId}，位置：$pos")
                val service =
                    MicroContext.getServiceManager().findServiceByInterface<AbsPanelCallerService>(
                        AbsPanelCallerService::class.java.name
                    )
                // 'context' 来自 SmartHomeView 的成员变量 context
                val activity = context.findActivity()
                if (activity != null) {
                    service.goPanelWithCheckAndTip(activity, device.devId)
                } else {
                    Log.e(TAG, "未找到 Activity 上下文，无法为设备 ${device.devId} 打开面板")
                    // 可以考虑在这里给用户一个 Toast 提示
                    // android.widget.Toast.makeText(context, "无法打开设备面板", android.widget.Toast.LENGTH_SHORT).show()
                }


                //跳转设备详情
//                val urlBuilder: UrlBuilder = UrlBuilder(activity, "panelMore")
//                val bundle: Bundle = Bundle()
//                bundle.putString("extra_panel_dev_id", device.devId)
//                bundle.putString("extra_panel_name", device.name)
//                urlBuilder.putExtras(bundle)
//                UrlRouter.execute(urlBuilder)

            }
        }
        contentRecyclerView = ExpandedHeightRecyclerView(context).apply {
            layoutManager = object : GridLayoutManager(context, 2) {
                override fun canScrollVertically() = false
            }.also { it.isAutoMeasureEnabled = true }
            adapter = deviceCardAdapter
            isNestedScrollingEnabled = false
            setHasFixedSize(false)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = 16f.dp }
            addItemDecoration(object : RecyclerView.ItemDecoration() {
                override fun getItemOffsets(
                    outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State
                ) {
                    val position = parent.getChildAdapterPosition(view)
                    val spanCount = 2
                    val spacing = 12f.dp
                    val column = position % spanCount
                    outRect.left = if (column == 0) 16f.dp else spacing / 2
                    outRect.right = if (column == spanCount - 1) 16f.dp else spacing / 2
                    outRect.top = spacing / 2
                    outRect.bottom = spacing / 2
                }
            })
        }
        container.addView(contentRecyclerView)

        scrollView.addView(container)
        addView(scrollView)
        setBackgroundColor(Color.WHITE)
        Log.d(TAG, "SmartHomeView UI 构建完成")
    }

    /** 2. 创建悬浮卡片和阴影的组合 View */
    private fun createOverlappingCard(cardHeight: Int, overlapOffset: Int): FrameLayout {
        // Card 内容
        val card = MaterialCardView(context).apply {
            strokeWidth = 0
            radius = 8f.dp.toFloat()
            cardElevation = 12f.dp.toFloat()
            useCompatPadding = true
            setCardBackgroundColor(Color.WHITE)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                outlineAmbientShadowColor = 0x55000000
                outlineSpotShadowColor = 0x55000000
            }
            removeAllViews()
            addView(LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(16f.dp, 0, 16f.dp, 0)

                // 左侧图标
                addView(ImageView(context).apply {
                    setImageResource(R.drawable.myhome)
                    layoutParams = LinearLayout.LayoutParams(24f.dp, 24f.dp)
                })
                // 中间文字
                addView(
                    TextView(context).apply {
                        homeTitleTextView = this               // 2. 保存引用
                        layoutParams = LinearLayout.LayoutParams(
                            0,
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            1f
                        )
                        // 让文字在该 View 里垂直居中
                        gravity = Gravity.CENTER_VERTICAL
                        // 文本内容
                        text = homeName ?: "暂无家庭信息"       // 初始显示
                        textSize = 16f
                        setTextColor(Color.BLACK)
                        // 左右内边距保持和之前一致
                        setPadding(16f.dp, 0, 16f.dp, 0)
                    }
                )
                // 右侧切换
                addView(ImageView(context).apply {
                    setImageResource(R.drawable.change)
                    layoutParams = LinearLayout.LayoutParams(32f.dp, 32f.dp)
                    setOnClickListener {
                        // —— ① 在启动 MyHomeActivity 之前，把回调写好 ——
                        MyHomeActivity.onSelected = { homeName, homeId ->
                            Toast.makeText(context, "选中了：$homeName (ID=$homeId)", Toast.LENGTH_SHORT).show()
                            this@SmartHomeView.homeId   = homeId
                            this@SmartHomeView.homeName = homeName
                            //开始刷新 UI
                            homeTitleTextView.post {
                                homeTitleTextView.text = homeName
                            }
                            val service =
                                MicroServiceManager.getInstance().findServiceByInterface<AbsBizBundleFamilyService>(
                                    AbsBizBundleFamilyService::class.java.name
                                )

                            // 设置为当前家庭的 homeId 和 homeName
                            service.shiftCurrentFamily(homeId, homeName)
                            requestTuyaRoomData()

                        }

                        context.startActivity(Intent(context, MyHomeActivity::class.java))
                    }
                })
            })
        }




        // Wrapper + 阴影
        return FrameLayout(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                cardHeight
            ).apply { leftMargin = 16f.dp; rightMargin = 16f.dp; topMargin = -overlapOffset }
            addView(card, LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT
            ))
            addView(View(context).apply {
                layoutParams = LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    8f.dp, Gravity.BOTTOM
                )
                background = GradientDrawable(
                    GradientDrawable.Orientation.TOP_BOTTOM,
                    intArrayOf(0x22000000, 0x00000000)
                )
            })
        }
    }

    /** 3. 创建标题行 */
    private fun createTitleRow(): LinearLayout {
        val titleRow = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT
            ).apply { topMargin = 16f.dp }
            gravity = Gravity.CENTER_VERTICAL
            isClickable = true; isFocusable = true
            val ta = context.obtainStyledAttributes(intArrayOf(android.R.attr.selectableItemBackground))
            background = ContextCompat.getDrawable(context, ta.getResourceId(0,0))
            ta.recycle()
            setOnClickListener { Log.d(TAG, "点击 “智能化设备”") }
        }
        titleRow.addView(TextView(context).apply {
            text = "智能化设备"
            paint.isFakeBoldText = true
            textSize = 16f
            setTextColor(Color.BLACK)
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
                .apply { marginStart = 12f.dp }
        })
//        titleRow.addView(TextView(context).apply {
//            text = "全部设备 >"
//            textSize = 14f
//            setTextColor(Color.parseColor("#888888"))
//            layoutParams = LinearLayout.LayoutParams(
//                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT
//            ).apply { marginEnd = 12f.dp }
//        })
        return titleRow
    }

    private fun initData() {
        requestTuyaHomeData()
    }

    //获取家庭组
    private fun requestTuyaHomeData() {
        ThingHomeSdk.getHomeManagerInstance().queryHomeList(object : IThingGetHomeListCallback {
            override fun onSuccess(homeBeans: List<HomeBean>) {
                //打印家庭列表
                Log.d(TAG, "获取家庭列表成功，数量: ${homeBeans.size}")
                // 如果列表不为空，就取第一个来演示
                if (homeBeans.isNotEmpty()) {
                    val firstHome = homeBeans[0]
                    // 给外层的属性赋值
                    this@SmartHomeView.homeId   = firstHome.homeId
                    this@SmartHomeView.homeName = firstHome.name
                    //开始刷新 UI
                    homeTitleTextView.post {
                        homeTitleTextView.text = homeName
                    }
                    val service =
                        MicroServiceManager.getInstance().findServiceByInterface<AbsBizBundleFamilyService>(
                            AbsBizBundleFamilyService::class.java.name
                        )

                    // 设置为当前家庭的 homeId 和 homeName
                    service.shiftCurrentFamily(homeId, homeName)
                    requestTuyaRoomData()

                }
            }

            override fun onError(errorCode: String, error: String) {
                // do something
            }
        })
    }

    //获取该家庭组下的房间列表
    private fun requestTuyaRoomData() {

        ThingHomeSdk.newHomeInstance(homeId).getHomeDetail(object : IThingHomeResultCallback {
            override fun onSuccess(bean: HomeBean) {
                // 拿到房间数组
                rooms = bean.rooms
                // 刷新按钮 UI，一定要在主线程：
                buttonContainer.post {
                    // 清空旧的
                    buttons.clear()
                    buttonContainer.removeAllViews()

                    // 用新数据生成按钮
                    rooms.forEachIndexed { idx, room ->
                        Button(context).apply {
                            text = room.name
                            isAllCaps = false
                            setBackgroundColor(Color.TRANSPARENT)
                            setPadding(10f.dp,8f.dp,10f.dp,8f.dp)
                            layoutParams = LinearLayout.LayoutParams(
                                ViewGroup.LayoutParams.WRAP_CONTENT,
                                ViewGroup.LayoutParams.WRAP_CONTENT
                            ).apply { marginEnd = 4f.dp }
                            setOnClickListener { selectPage(rooms[idx],idx) }
                        }.also { btn ->
                            buttons.add(btn)
                            buttonContainer.addView(btn)
                        }
                    }
                    // 默认选中第一个房间
                    if (buttons.isNotEmpty()) selectPage(rooms[0],0)
                }
            }
            override fun onError(errorCode: String, errorMsg: String) {
                Log.e(TAG, "获取房间列表失败：$errorCode / $errorMsg")
            }
        })
    }

    //根据roomID 获取设备列表
    private fun requestTuyaDeviceData(deviceBean: MutableList<DeviceBean>) {
        // 更新 Adapter 的数据
        deviceCardAdapter.updateData(deviceBean)
    }

    //设备配网
    private fun requestTuyaDeviceConfig() {
        val activity = context.findActivity() ?: run {
            Log.e(TAG, "找不到 Activity，无法启动配网")
            return
        }
        ThingDeviceActivatorManager.addListener(object : IThingDeviceActiveListener {
            override fun onDevicesAdd(list: List<String?>?) {
                Log.d(TAG, "onDevicesAdd -> $list")
            }
            override fun onRoomDataUpdate() {
                Log.d(TAG, "onRoomDataUpdate")
            }
            override fun onOpenDevicePanel(s: String) {
                Log.d(TAG, "onOpenDevicePanel -> $s")
            }

        })
        ThingDeviceActivatorManager.startDeviceActiveAction(activity)
    }
    /**
     * 递归 unwrap ContextWrapper，直到找到 Activity 或者返回 null
     */
    private fun Context.findActivity(): Activity? {
        var ctx = this
        while (ctx is ContextWrapper) {
            if (ctx is Activity) return ctx
            ctx = ctx.baseContext
        }
        return null
    }

    /** 4. 选中页面，更新按钮状态和数据 */
    private fun selectPage(roomBean: RoomBean, index: Int) {
        if (index !in buttons.indices) return
        buttons.forEachIndexed { i, btn ->
            btn.setTextColor(if (i == index) Color.BLACK else Color.GRAY)
            btn.setTypeface(null, if (i == index) Typeface.BOLD else Typeface.NORMAL)
        }
        Log.d(TAG, "选中 $index，更新数据")
        requestTuyaDeviceData(roomBean.deviceList)
    }


    /** 6. 释放资源 */
    fun cleanup() {
        Log.d(TAG, "cleanup() 释放资源")
        if (::buttonContainer.isInitialized) buttonContainer.removeAllViews()
        if (::contentRecyclerView.isInitialized) contentRecyclerView.adapter = null
        buttons.clear()
    }

    /** 自动展开高度的 RecyclerView，适合放在 ScrollView 里 */
    private class ExpandedHeightRecyclerView(context: Context) : RecyclerView(context) {
        override fun onMeasure(widthSpec: Int, heightSpec: Int) {
            val expandedSpec = MeasureSpec.makeMeasureSpec(
                Int.MAX_VALUE shr 2,
                MeasureSpec.AT_MOST
            )
            super.onMeasure(widthSpec, expandedSpec)
        }
    }
}