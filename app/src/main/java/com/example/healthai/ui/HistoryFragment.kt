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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 分析历史列表（R3 折叠展开）。
 *
 * 卡片改为内联折叠/展开，统一消费 [com.example.healthai.util.ResultFormatter.displayTextFor]；
 * 旧记录（displayText 为空）自动回退为可读中文，不再直接展示原始 JSON，也不再弹出详情 Dialog。
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
            binding.rvHistory.adapter = HistoryAdapter(list)
        }
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}
