package com.example.healthai.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.healthai.R
import com.example.healthai.data.AnalysisRecord
import com.example.healthai.databinding.ItemHistoryBinding
import com.example.healthai.util.ImageUtils
import com.example.healthai.util.ResultFormatter
import com.google.gson.Gson
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 历史卡片适配器（R5）。
 *
 * 相较 R4 的变化：
 * - 文字分析结果仅显示前 5 行（布局 maxLines=5 + ellipsize），点击文字或提示打开全屏详情页；
 * - 不足四张图时，剩余缩略图显示**透明色块**占位（bg_thumb_placeholder：透明填充 + 浅灰描边）；
 *   有图的缩略图不显示边框（背景置 null）。
 * - 每张卡片仍提供「删除」按钮，点击通过 [onDelete] 回调给 [HistoryFragment] 处理。
 */
class HistoryAdapter(
    private val items: List<AnalysisRecord>,
    private val onDelete: (AnalysisRecord) -> Unit,
    private val onOpenDetail: (title: String, text: String) -> Unit
) : RecyclerView.Adapter<HistoryAdapter.VH>() {

    class VH(val binding: ItemHistoryBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemHistoryBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(binding)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = items[position]
        val ctx = holder.itemView.context

        // 时间 + 档案名（顶行左侧）
        val time = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.CHINA).format(Date(item.createdAt))
        val meta = buildString {
            append(time)
            if (item.profileName.isNotBlank()) append(" · ${item.profileName}")
        }
        val typeLabel = if (item.type == "body") "身材" else "食物"
        holder.binding.tvMeta.text = meta
        holder.binding.tvType.text = typeLabel

        // 四张缩略图：有图显示（无边框），缺图显示透明占位（透明填充 + 浅灰描边）
        val placeholder = ContextCompat.getDrawable(ctx, R.drawable.bg_thumb_placeholder)
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
            if (bmp != null) {
                thumbs[i].setImageBitmap(bmp)
                thumbs[i].background = null
            } else {
                thumbs[i].setImageDrawable(null)
                thumbs[i].background = placeholder
            }
        }

        // 完整文字分析结果：仅显示前 5 行；点击文字或提示打开全屏详情页
        val fullText = ResultFormatter.displayTextFor(item)
        val title = "$meta · $typeLabel"
        holder.binding.tvText.text = fullText
        holder.binding.tvText.setOnClickListener { onOpenDetail(title, fullText) }
        holder.binding.tvTextHint.setOnClickListener { onOpenDetail(title, fullText) }

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
