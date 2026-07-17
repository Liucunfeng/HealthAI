package com.example.healthai.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.healthai.data.AnalysisRecord
import com.example.healthai.databinding.ItemHistoryBinding
import com.example.healthai.util.ImageUtils
import com.example.healthai.util.ResultFormatter
import com.google.gson.Gson
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 历史卡片适配器（R3 折叠展开）。
 *
 * - 折叠态：仅显示 [ItemHistoryBinding.tvSummary]（= [AnalysisRecord.summary] 一句话总体评价）、
 *   时间、类型与朝下的箭头 [ItemHistoryBinding.ivArrow]；[ItemHistoryBinding.tvDetail] 不可见。
 * - 展开态：在折叠态基础上显示 [ItemHistoryBinding.tvDetail]（= [ResultFormatter.displayTextFor]
 *   全文，含首行），箭头旋转 180° 朝上。
 *
 * 点击 itemView 在内部切换 per-position 展开状态并局部刷新（[RecyclerView.Adapter.notifyItemChanged]），
 * 不再回调外部 [com.example.healthai.ui.HistoryFragment.showDetail] 弹窗。
 */
class HistoryAdapter(
    private val items: List<AnalysisRecord>
) : RecyclerView.Adapter<HistoryAdapter.VH>() {

    /** 每个位置的展开状态，初始全折叠；随 adapter 重建重置（每次进入历史页默认折叠）。 */
    private val expanded: MutableList<Boolean> = MutableList(items.size) { false }

    class VH(val binding: ItemHistoryBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemHistoryBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(binding)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = items[position]
        val isExpanded = expanded[position]

        // 折叠态：one-liner 总体评价（共享约定 #1）
        holder.binding.tvSummary.text = item.summary
        // 展开态：完整可读中文（共享约定 #2，允许与 summary 轻微重复）
        holder.binding.tvDetail.text = ResultFormatter.displayTextFor(item)
        holder.binding.tvDetail.visibility = if (isExpanded) View.VISIBLE else View.GONE
        // 箭头旋转：折叠 0°（朝下）/ 展开 180°（朝上）（共享约定 #3）
        holder.binding.ivArrow.rotation = if (isExpanded) 180f else 0f

        // 时间格式化（复用现有格式）
        val time = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.CHINA).format(Date(item.createdAt))
        holder.binding.tvMeta.text = buildString {
            append(time)
            if (item.profileName.isNotBlank()) append(" · ${item.profileName}")
        }
        holder.binding.tvType.text = if (item.type == "body") "身材" else "食物"

        // 最多 4 张缩略图：有图显示，缺图保持灰色色块占位
        val imgs = imagesOf(item)
        val thumbs = listOf(
            holder.binding.ivThumb0,
            holder.binding.ivThumb1,
            holder.binding.ivThumb2,
            holder.binding.ivThumb3
        )
        for (i in thumbs.indices) {
            val b64 = imgs.getOrNull(i)
            val bmp = if (!b64.isNullOrBlank()) ImageUtils.base64ToBitmap(b64) else null
            if (bmp != null) thumbs[i].setImageBitmap(bmp) else thumbs[i].setImageDrawable(null)
        }

        // 点击内联切换折叠/展开
        holder.itemView.setOnClickListener {
            expanded[position] = !expanded[position]
            notifyItemChanged(position)
        }
    }

    /**
     * 取出该记录的全部图片 base64 列表（最多 4 张）。
     * 新记录读 imageListJson；旧记录 imageListJson 为空时回退为 imageBase64 首图。
     */
    private fun imagesOf(rec: AnalysisRecord): List<String> {
        if (rec.imageListJson.isNotBlank()) {
            runCatching {
                Gson().fromJson(rec.imageListJson, Array<String>::class.java)?.toList()
            }.getOrNull()?.let { return it }
        }
        return if (rec.imageBase64.isNotBlank()) listOf(rec.imageBase64) else emptyList()
    }
}
