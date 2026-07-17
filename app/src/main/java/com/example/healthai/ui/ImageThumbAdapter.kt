package com.example.healthai.ui

import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.healthai.databinding.ItemImageThumbBinding

/**
 * 选人下拉的选项（id 与展示名）。
 * id == 0 表示"通用建议（不指定）"。
 */
data class ProfileOption(val id: Long, val name: String)

/**
 * 多图选择缩略适配器（R1 多图分析）。
 * 展示已选图片（4:3）+ 末尾「添加」格（最多 4 张时隐藏添加格）。
 *
 * @param uris    已选图片 Uri 列表（与 Fragment 中的同一可变列表，增删后 notifyDataSetChanged）
 * @param onAdd   点击「添加」格回调
 * @param onRemove 点击某张图删除按钮回调，参数为其在 uris 中的下标
 */
class ImageThumbAdapter(
    private val uris: MutableList<Uri>,
    private val onAdd: () -> Unit,
    private val onRemove: (Int) -> Unit
) : RecyclerView.Adapter<ImageThumbAdapter.VH>() {

    class VH(val binding: ItemImageThumbBinding) : RecyclerView.ViewHolder(binding.root)

    private val canAdd: Boolean get() = uris.size < 4

    override fun getItemCount(): Int = uris.size + if (canAdd) 1 else 0

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemImageThumbBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(binding)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        if (position < uris.size) {
            // 图片项
            holder.binding.ivThumb.setImageURI(uris[position])
            holder.binding.tvAdd.visibility = View.GONE
            holder.binding.btnRemove.visibility = View.VISIBLE
            holder.binding.root.setOnClickListener(null)
            holder.binding.btnRemove.setOnClickListener { onRemove(position) }
        } else {
            // 添加格
            holder.binding.ivThumb.setImageURI(null)
            holder.binding.tvAdd.visibility = View.VISIBLE
            holder.binding.btnRemove.visibility = View.GONE
            holder.binding.btnRemove.setOnClickListener(null)
            holder.binding.root.setOnClickListener { onAdd() }
        }
    }
}
