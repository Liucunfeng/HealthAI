package com.example.healthai.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.healthai.data.AppDatabase
import com.example.healthai.databinding.FragmentHistoryBinding
import com.example.healthai.util.ResultFormatter
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 分析历史列表（R4 历史净化）。
 * 卡片与详情均统一消费 ResultFormatter.displayTextFor，旧记录（displayText 为空）
 * 自动回退为可读中文，不再直接展示原始 JSON。
 */
class HistoryFragment : Fragment() {

    private var _binding: FragmentHistoryBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHistoryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.rvHistory.layoutManager = LinearLayoutManager(requireContext())
    }

    override fun onResume() {
        super.onResume()
        load()
    }

    private fun load() {
        lifecycleScope.launch {
            val list = withContext(Dispatchers.IO) {
                AppDatabase.get(requireContext()).analysisRecordDao().getAll()
            }
            binding.tvEmpty.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
            binding.rvHistory.adapter = HistoryAdapter(list) { showDetail(it) }
        }
    }

    /** 详情弹窗展示可读中文（displayText 优先，旧记录回退） */
    private fun showDetail(rec: com.example.healthai.data.AnalysisRecord) {
        val title = if (rec.type == "body") "身材分析详情" else "食物分析详情"
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(title)
            .setMessage(ResultFormatter.displayTextFor(rec))
            .setPositiveButton(android.R.string.ok, null)
            .show()
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}
