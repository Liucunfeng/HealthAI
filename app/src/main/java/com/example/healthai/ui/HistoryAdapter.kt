package com.example.healthai.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.healthai.data.AnalysisRecord
import com.example.healthai.databinding.ItemHistoryBinding
import com.example.healthai.util.ImageUtils
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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
        holder.binding.tvSummary.text = item.summary.ifBlank { "(无摘要)" }
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
