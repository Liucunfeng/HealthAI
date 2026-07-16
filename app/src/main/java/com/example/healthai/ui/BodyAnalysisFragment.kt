package com.example.healthai.ui

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.healthai.R
import com.example.healthai.data.AnalysisRecord
import com.example.healthai.data.AppDatabase
import com.example.healthai.data.AppPreferences
import com.example.healthai.databinding.FragmentBodyBinding
import com.example.healthai.util.ImageUtils
import com.example.healthai.vision.BodyAnalysis
import com.example.healthai.vision.VisionAnalyzerFactory
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class BodyAnalysisFragment : Fragment() {

    private var _binding: FragmentBodyBinding? = null
    private val binding get() = _binding!!
    private var currentBase64: String = ""
    private var pendingUri: Uri? = null

    private val takePhoto = registerForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.TakePicture()
    ) { ok ->
        if (ok) {
            pendingUri?.let {
                currentBase64 = ImageUtils.uriToCompressedBase64(requireContext(), it)
                binding.ivPreview.setImageURI(it)
            }
        }
    }

    private val pickImage = registerForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            currentBase64 = ImageUtils.uriToCompressedBase64(requireContext(), it)
            binding.ivPreview.setImageURI(it)
        }
    }

    private val requestCamera = registerForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) openCamera()
        else Toast.makeText(requireContext(), R.string.permission_required, Toast.LENGTH_SHORT).show()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentBodyBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.btnCapture.setOnClickListener { ensurePermissionThenCamera() }
        binding.btnPick.setOnClickListener { ensurePermissionThenPick() }
        binding.btnAnalyze.setOnClickListener { analyze() }
    }

    private fun ensurePermissionThenCamera() {
        val granted = ContextCompat.checkSelfPermission(
            requireContext(), Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
        if (granted) openCamera()
        else requestCamera.launch(Manifest.permission.CAMERA)
    }

    private fun ensurePermissionThenPick() {
        pickImage.launch("image/*")
    }

    private fun openCamera() {
        val file = File(requireContext().externalCacheDir, "capture_body_${System.currentTimeMillis()}.jpg")
        pendingUri = FileProvider.getUriForFile(
            requireContext(),
            "${requireContext().packageName}.fileprovider",
            file
        )
        takePhoto.launch(pendingUri)
    }

    private fun analyze() {
        if (currentBase64.isBlank()) {
            Toast.makeText(requireContext(), R.string.error_no_image, Toast.LENGTH_SHORT).show()
            return
        }
        if (AppPreferences.getApiKey(requireContext()).isBlank()) {
            Toast.makeText(requireContext(), R.string.warn_no_key, Toast.LENGTH_SHORT).show()
            return
        }
        lifecycleScope.launch {
            binding.progress.visibility = View.VISIBLE
            binding.btnAnalyze.isEnabled = false
            try {
                val profile = withContext(Dispatchers.IO) {
                    AppDatabase.get(requireContext()).userProfileDao().get()
                }
                val analyzer = VisionAnalyzerFactory.create(requireContext())
                    ?: throw IllegalStateException("no analyzer")
                val result = withContext(Dispatchers.IO) {
                    analyzer.analyzeBody(currentBase64, profile)
                }
                showResult(result)
                val rec = AnalysisRecord(
                    type = "body",
                    createdAt = System.currentTimeMillis(),
                    imageBase64 = currentBase64,
                    summary = result.overall,
                    detailJson = Gson().toJson(result)
                )
                withContext(Dispatchers.IO) {
                    AppDatabase.get(requireContext()).analysisRecordDao().insert(rec)
                }
            } catch (e: Exception) {
                android.util.Log.e("HealthAI", "analyze failed", e)
                val detail = e.message?.takeIf { it.isNotBlank() }
                Toast.makeText(
                    requireContext(),
                    detail?.let { "分析失败：$it" } ?: getString(R.string.error_generic),
                    Toast.LENGTH_LONG
                ).show()
            } finally {
                binding.progress.visibility = View.GONE
                binding.btnAnalyze.isEnabled = true
            }
        }
    }

    private fun showResult(r: BodyAnalysis) {
        val sb = StringBuilder()
        sb.appendLine("总体评价：${r.overall}")
        if (r.bodyType.isNotBlank()) sb.appendLine("体型：${r.bodyType}")
        if (r.proportions.isNotEmpty()) {
            sb.appendLine("\n比例指标：")
            r.proportions.forEach { sb.appendLine("· ${it.label}：${it.value} —— ${it.comment}") }
        }
        if (r.physique.isNotBlank()) sb.appendLine("\n体质：${r.physique}")
        if (r.bodyFatEstimate.isNotBlank()) sb.appendLine("体脂估算：${r.bodyFatEstimate}")
        if (r.advice.isNotEmpty()) {
            sb.appendLine("\n建议：")
            r.advice.forEach { sb.appendLine("· $it") }
        }
        binding.tvResult.text = sb.toString()
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}
