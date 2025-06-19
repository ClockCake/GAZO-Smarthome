package com.jiyoujiaju.jijiahui

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.AbsListView
import android.widget.BaseAdapter
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.RadioButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.tekartik.sqflite.Constant.TAG
import com.thingclips.smart.api.router.UrlRouter
import com.thingclips.smart.home.sdk.ThingHomeSdk
import com.thingclips.smart.home.sdk.bean.HomeBean
import com.thingclips.smart.home.sdk.callback.IThingGetHomeListCallback

class MyHomeActivity : AppCompatActivity() {

    companion object {
        /**
         * 这是一个静态回调：
         * 当用户在 MyHomeActivity 里选完某个 “家” 并且点“确定”时，
         * Activity 会去调用它，并把 (homeName, homeId) “推回去”给调用方。
         *
         * 使用方式（在启动 MyHomeActivity 之前）：
         *   MyHomeActivity.onSelected = { name, id ->
         *       // name: String，id: Long
         *       // 在这里处理“用户选中了哪个家”
         *   }
         */
        var onSelected: ((homeName: String, homeId: Long) -> Unit)? = null
    }

    private lateinit var confirmButton: Button  // 确认按钮
    private lateinit var managerButton: Button // 管理按钮（如果需要的话，可以添加）
    private lateinit var listView: ListView
    private var selectedPosition: Int = -1
    private var homeBeans: List<HomeBean> = emptyList()
    private val items = mutableListOf<String>() // 显示各个 HomeBean 的 name

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT
            )
        }

        // ─── 顶部导航栏 ──────────────────────────────
        val topBar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(16.dp, 16.dp, 16.dp, 16.dp)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        val titleView = TextView(this).apply {
            text = "我的家"
            textSize = 20f
            layoutParams = LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f)
        }

        confirmButton = Button(this).apply {
            text = "确定"
            isEnabled = false
            setTextColor(Color.GRAY)
        }

        managerButton = Button(this).apply {
            text = "家庭管理"
            setOnClickListener {
                // 确保已经正确导入 UrlRouter 和 FamilyManageActivity
                UrlRouter.execute(UrlRouter.makeBuilder(context, "family_manage"))
            }
        }

        topBar.addView(titleView)
        topBar.addView(confirmButton)
        topBar.addView(managerButton)
        layout.addView(topBar)

        // ─── ListView ──────────────────────────────
        listView = ListView(this).apply {
            adapter = object : BaseAdapter() {
                override fun getCount(): Int = items.size
                override fun getItem(position: Int): Any = items[position]
                override fun getItemId(position: Int): Long = position.toLong()
                override fun getView(position: Int, convertView: android.view.View?, parent: ViewGroup?): android.view.View {
                    val row = LinearLayout(this@MyHomeActivity).apply {
                        orientation = LinearLayout.HORIZONTAL
                        setPadding(24.dp, 24.dp, 24.dp, 24.dp)
                        layoutParams = AbsListView.LayoutParams(
                            AbsListView.LayoutParams.MATCH_PARENT,
                            AbsListView.LayoutParams.WRAP_CONTENT
                        )
                        gravity = Gravity.CENTER_VERTICAL
                    }

                    val textView = TextView(this@MyHomeActivity).apply {
                        text = items[position]
                        layoutParams = LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f)
                    }

                    val radioButton = RadioButton(this@MyHomeActivity).apply {
                        isChecked = position == selectedPosition
                        isClickable = false
                    }

                    row.addView(textView)
                    row.addView(radioButton)

                    row.setOnClickListener {
                        // 选中当前项
                        selectedPosition = position
                        notifyDataSetChanged()
                        confirmButton.isEnabled = true
                        confirmButton.setTextColor(Color.BLACK)
                    }

                    return row
                }
            }
        }

        layout.addView(listView)
        setContentView(layout)

        // ─── 确认按钮的点击逻辑 ────────────────────────
        confirmButton.setOnClickListener {
            if (selectedPosition in homeBeans.indices) {
                val selectedHome = homeBeans[selectedPosition]
                Log.d(TAG, "确定选择的家庭: 名称=${selectedHome.name}, ID=${selectedHome.homeId}")

                // —— 调用静态回调，把结果推回给“调用者” ——
                onSelected?.invoke(selectedHome.name, selectedHome.homeId)

                // 使用完之后清空，避免内存泄漏
                onSelected = null

                // 结束自己
                finish()
            } else {
                // 理论上不应该走到这里，因为只有选中后按钮才可点击
                Toast.makeText(this, "请选择一个家庭后再确定", Toast.LENGTH_SHORT).show()
            }
        }

        // 开始向 Tuya 拉取用户家庭列表
//        requestTuyaHomeData()
    }


    override fun onResume() {
        super.onResume()
        // 每次 Activity 可见时，都重新拉取一次“家”的列表
        // → 先重置选中状态和按钮状态
        selectedPosition = -1
        confirmButton.isEnabled = false
        confirmButton.setTextColor(Color.GRAY)

        // 然后再拉数据，onSuccess 里会填充 items 并 notifyDataSetChanged()
        requestTuyaHomeData()
    }

    override fun onDestroy() {
        super.onDestroy()
        // 清理静态回调，避免内存泄漏
        onSelected = null
    }
    // 从 Tuya 拿到用户的 HomeBean 列表，填充到 ListView
    private fun requestTuyaHomeData() {
        ThingHomeSdk.getHomeManagerInstance().queryHomeList(object : IThingGetHomeListCallback {
            override fun onSuccess(beans: List<HomeBean>) {
                Log.d(TAG, "获取家庭列表成功，数量: ${beans.size}")
                homeBeans = beans
                items.clear()
                items.addAll(beans.map { it.name })
                (listView.adapter as BaseAdapter).notifyDataSetChanged()
            }

            override fun onError(errorCode: String, error: String) {
                Log.e(TAG, "获取家庭列表失败: $errorCode - $error")
            }
        })
    }

    // dp 扩展属性
    private val Int.dp: Int get() = (this * resources.displayMetrics.density).toInt()
}