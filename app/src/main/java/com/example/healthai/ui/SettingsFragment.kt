package com.example.healthai.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.healthai.BuildConfig
import com.example.healthai.R
import com.example.healthai.data.AppDatabase
import com.example.healthai.data.AppPreferences
import com.example.healthai.databinding.FragmentSettingsBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.etApiKey.setText(AppPreferences.getApiKey(requireContext()))
        binding.etApiBase.setText(AppPreferences.getApiBase(requireContext()))
        binding.etModel.setText(AppPreferences.getModel(requireContext()))

        // 居中显示软件版本（取自 BuildConfig，始终与 build.gradle 的 versionName 同步）
        binding.tvVersion.text = getString(R.string.app_version, BuildConfig.VERSION_NAME)

        binding.btnSaveSettings.setOnClickListener {
            AppPreferences.setApiKey(requireContext(), binding.etApiKey.text.toString().trim())
            AppPreferences.setApiBase(requireContext(), binding.etApiBase.text.toString().trim())
            val model = binding.etModel.text.toString().trim().ifBlank { "gpt-4o-mini" }
            AppPreferences.setModel(requireContext(), model)
            Toast.makeText(requireContext(), R.string.settings_saved, Toast.LENGTH_SHORT).show()
        }

        binding.btnClearHistory.setOnClickListener {
            lifecycleScope.launch {
                withContext(Dispatchers.IO) {
                    AppDatabase.get(requireContext()).analysisRecordDao().clear()
                }
                Toast.makeText(requireContext(), R.string.history_empty, Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}
