import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.android.material.card.MaterialCardView
import com.thingclips.smart.sdk.bean.DeviceBean

// RecyclerView Adapter，用于显示设备卡片，图片和文本由外部传入
class DeviceCardAdapter(
    private val context: Context,
    private var items: List<DeviceBean>
) : RecyclerView.Adapter<DeviceCardAdapter.DeviceViewHolder>() {

    /** 1. 点击回调，高阶函数 */
    var onItemClick: ((device: DeviceBean, position: Int) -> Unit)? = null

    /**
     * 更新数据列表并刷新
     */
    @SuppressLint("NotifyDataSetChanged")
    fun updateData(newItems: List<DeviceBean>) {
        this.items = newItems
        notifyDataSetChanged()
    }

    /**
     * 创建 ViewHolder，动态构建卡片布局
     */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DeviceViewHolder {
        val density = context.resources.displayMetrics.density

        // 根容器：MaterialCardView
        val cardView = MaterialCardView(context).apply {
            val heightPx = (140 * density).toInt()
            val marginPx = (4 * density).toInt()
            layoutParams = RecyclerView.LayoutParams(
                RecyclerView.LayoutParams.MATCH_PARENT,
                heightPx
            ).apply {
                setMargins(marginPx, marginPx, marginPx, marginPx)
            }
            radius = (12 * density)
            cardElevation = (4 * density)
        }

        val verticalGroup = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                gravity = Gravity.CENTER_VERTICAL
                marginStart = (12 * density).toInt()
            }
        }

        val imageSize = (60 * density).toInt()
        val imageView = ImageView(context).apply {
            id = View.generateViewId()
            layoutParams = LinearLayout.LayoutParams(imageSize, imageSize)
            scaleType = ImageView.ScaleType.CENTER_CROP
        }

        val textView = TextView(context).apply {
            id = View.generateViewId()
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = (8 * density).toInt() }
            textAlignment = View.TEXT_ALIGNMENT_CENTER
            setTextColor(Color.BLACK)
            textSize = 12f
        }

        val isOnlineTextView = TextView(context).apply {
            id = View.generateViewId()
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = (8 * density).toInt() }
            textAlignment = View.TEXT_ALIGNMENT_CENTER
            textSize = 12f
        }

        verticalGroup.addView(imageView)
        verticalGroup.addView(textView)
        verticalGroup.addView(isOnlineTextView)
        cardView.addView(verticalGroup)

        return DeviceViewHolder(cardView, imageView, textView, isOnlineTextView)
    }

    /**
     * 绑定数据到 ViewHolder，同时设置点击事件
     */
    override fun onBindViewHolder(holder: DeviceViewHolder, position: Int) {
        val gray = 0xF5
        val bgColor = Color.argb(255, gray, gray, gray)
        (holder.itemView as MaterialCardView).setCardBackgroundColor(bgColor)

        val item = items[position]

        Glide.with(context)
            .load(item.iconUrl)
            .into(holder.imageView)

        holder.textView.text = item.name
        if (item.isOnline == true) {
            holder.isOnlineTextView.text = "在线"
            holder.isOnlineTextView.setTextColor(Color.parseColor("#00C08A"))
        } else {
            holder.isOnlineTextView.text = "已离线"
            holder.isOnlineTextView.setTextColor(Color.parseColor("#999999"))
        }

        // —— 这里设置点击回调 ——
        holder.itemView.setOnClickListener {
            onItemClick?.invoke(item, position)
        }
    }

    override fun getItemCount(): Int = items.size

    /**
     * ViewHolder，持有卡片、图片和文本控件的引用
     */
    class DeviceViewHolder(
        itemView: MaterialCardView,
        val imageView: ImageView,
        val textView: TextView,
        val isOnlineTextView: TextView
    ) : RecyclerView.ViewHolder(itemView)
}