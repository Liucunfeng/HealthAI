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
 * 历史卡片适配器（R4）。
 *
 * 布局变化（相较 R3）：
 * - 四张缩略图等宽填充整行（centerCrop 等比放大），不足四张显示纯白色块占位；
 * - 完整文字分析结果移到四图下方一行、顶格左对齐（不再分右侧文字列/箭头折叠）；
 * - 每张卡片提供「删除」按钮，点击通过 [onDelete] 回调给 [HistoryFragment] 处理。
 */
class HistoryAdapter(
    private val items: List<AnalysisRecord>,
    private val onDelete: (AnalysisRecord) -> Unit
) : RecyclerView.Adapter<HistoryAdapter.VH>() {

    class VH(val binding: ItemHistoryBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemHistoryBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(binding)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = items[position]

        // 时间 + 档案名（顶行左侧）
        val time = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.CHINA).format(Date(item.createdAt))
        holder.binding.tvMeta.text = buildString {
            append(time)
            if (item.profileName.isNotBlank()) append(" · ${item.profileName}")
        }
        holder.binding.tvType.text = if (item.type == "body") "身材" else "食物"

        // 四张缩略图：有图显示，缺图保持纯白块（背景已在布局中设为白色）
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

        // 完整文字分析结果：移到四图下方，顶格左对齐
        holder.binding.tvText.text = ResultFormatter.displayTextFor(item)

        // 删除按钮
        holder.binding.btnDelete.setOnClickListener { onDelete(item) }
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
