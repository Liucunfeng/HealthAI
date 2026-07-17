package com.example.healthai.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.healthai.R
import com.example.healthai.data.UserProfile
import com.example.healthai.databinding.ItemProfileBinding

/**
 * 多人档案列表适配器（R2）。
 * 每个档案项展示名称（空名占位"未命名档案"）、关键指标，并提供「编辑 / 删除」操作。
 * 当前档案的选择在身材 / 食物页各自完成，本页不再设置「当前」。
 */
class ProfileAdapter(
    private var items: List<UserProfile>,
    private val onEdit: (UserProfile) -> Unit,
    private val onDelete: (UserProfile) -> Unit
) : RecyclerView.Adapter<ProfileAdapter.VH>() {

    class VH(val binding: ItemProfileBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemProfileBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(binding)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        val p = items[position]
        val ctx = holder.itemView.context
        val name = if (p.name.isBlank()) ctx.getString(R.string.unnamed_profile) else p.name
        holder.binding.tvName.text = name

        val goalText = when (p.goal) {
            "lose" -> "减脂"
            "maintain" -> "维持"
            "gain" -> "增肌"
            else -> p.goal
        }
        val metrics = buildString {
            append("身高 ${p.heightCm.toInt()}cm · 体重 ${p.weightKg.toInt()}kg")
            if (p.age > 0) append(" · ${p.age}岁")
            if (p.gender.isNotBlank()) append(" · ${if (p.gender == "male") "男" else "女"}")
            if (goalText.isNotBlank() && (p.goal.isNotBlank())) append(" · $goalText")
        }
        holder.binding.tvMetrics.text = metrics

        holder.binding.btnEdit.setOnClickListener { onEdit(p) }
        holder.binding.btnDelete.setOnClickListener { onDelete(p) }
    }

    /** 替换数据并刷新 */
    fun submit(list: List<UserProfile>) {
        items = list
        notifyDataSetChanged()
    }
}
