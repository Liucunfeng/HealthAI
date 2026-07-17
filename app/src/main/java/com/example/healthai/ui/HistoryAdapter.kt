package com.example.healthai.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.healthai.data.AnalysisRecord
import com.example.healthai.databinding.ItemHistoryBinding
import com.example.healthai.util.ImageUtils
import com.example.healthai.util.ResultFormatter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 历史卡片适配器（R4 历史净化）。
 * 摘要统一使用 ResultFormatter.displayTextFor：新记录直显 displayText，
 * 旧记录回退为可读中文；均不再出现 `{} : " ` 与英文 JSON 键。
 */
class HistoryAdapter(
    private val items: List<AnalysisRecord>,
    private val onClick: (AnalysisRecord) -> Unit
) : RecyclerView.Adapter<HistoryAdapter.VH>() {

    class VH(val binding: ItemHistoryBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemHistoryBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(binding)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = items[position]
        val display = ResultFormatter.displayTextFor(item)
        holder.binding.tvSummary.text = display.ifBlank { item.summary.ifBlank { "(无摘要)" } }
        val time = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.CHINA).format(Date(item.createdAt))
        holder.binding.tvMeta.text = time
        holder.binding.tvType.text = if (item.type == "body") "身材" else "食物"
        if (item.imageBase64.isNotBlank()) {
            val bmp = ImageUtils.base64ToBitmap(item.imageBase64)
            if (bmp != null) holder.binding.ivThumb.setImageBitmap(bmp)
        }
        holder.itemView.setOnClickListener { onClick(item) }
    }
}
