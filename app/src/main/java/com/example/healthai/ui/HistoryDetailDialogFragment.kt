package com.example.healthai.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams
import androidx.fragment.app.DialogFragment
import com.example.healthai.databinding.FragmentHistoryDetailBinding

/**
 * 历史详情全屏页（R5）。
 *
 * 从历史卡片点击被截断的文字后弹出，展示**完整**分析文本（含卡片里只显示前 5 行而被省略的部分）。
 * 以全屏 DialogFragment 实现，覆盖在历史页之上，关闭即返回原列表，不干扰底部导航。
 */
class HistoryDetailDialogFragment : DialogFragment() {

    private var _binding: FragmentHistoryDetailBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHistoryDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.tvDetailTitle.text = arguments?.getString(ARG_TITLE) ?: ""
        binding.tvDetailText.text = arguments?.getString(ARG_TEXT) ?: ""
        binding.btnDetailClose.setOnClickListener { dismiss() }
    }

    override fun onStart() {
        super.onStart()
        // 铺满整屏，作为独立"页面"；透明窗口背景避免默认对话框圆角/留白
        dialog?.window?.setLayout(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
        dialog?.window?.setBackgroundDrawableResource(android.R.color.transparent)
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    companion object {
        private const val ARG_TITLE = "arg_title"
        private const val ARG_TEXT = "arg_text"

        fun newInstance(title: String, text: String): HistoryDetailDialogFragment {
            return HistoryDetailDialogFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_TITLE, title)
                    putString(ARG_TEXT, text)
                }
            }
        }
    }
}
