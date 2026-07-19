package com.example.healthai.ui

import com.example.healthai.R
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.healthai.data.AnalysisRecord
import com.example.healthai.data.AppDatabase
import com.example.healthai.databinding.FragmentHistoryBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 分析历史列表（R4）。
 *
 * - 卡片内联展示四图 + 完整文字结果（详见 [HistoryAdapter]）；
 * - 支持单条删除：点「删除」→ 确认框 → 删库 → 刷新列表。
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
            binding.rvHistory.adapter = HistoryAdapter(
                items = list,
                onDelete = { rec -> confirmDelete(rec) },
                onOpenDetail = { title, text -> openDetail(title, text) }
            )
        }
    }

    /** 打开全屏详情页，展示完整分析文本。 */
    private fun openDetail(title: String, text: String) {
        HistoryDetailDialogFragment.newInstance(title, text)
            .show(childFragmentManager, "history_detail")
    }

    /** 删除前二次确认，避免误删。 */
    private fun confirmDelete(rec: AnalysisRecord) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.history_delete_title)
            .setMessage(R.string.history_delete_confirm)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                lifecycleScope.launch {
                    withContext(Dispatchers.IO) {
                        AppDatabase.get(requireContext()).analysisRecordDao().deleteById(rec.id)
                    }
                    load()
                }
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}
