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
import com.example.healthai.databinding.FragmentFoodBinding
import com.example.healthai.util.ImageUtils
import com.example.healthai.vision.FoodAnalysis
import com.example.healthai.vision.VisionAnalyzerFactory
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class FoodAnalysisFragment : Fragment() {

    private var _binding: FragmentFoodBinding? = null
    private val binding get() = _binding!!
    private var currentBase64: String = ""
    private var pendingUri: Uri? = null

    private val takePhoto = registerForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.TakePicture()
    ) { ok ->
        if (ok) {
            pendingUri?.let {
                currentBase64 = ImageUtils.uriToCompressedBase64(requireContext(), it)
                binding.ivFoodPreview.setImageURI(it)
            }
        }
    }

    private val pickImage = registerForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            currentBase64 = ImageUtils.uriToCompressedBase64(requireContext(), it)
            binding.ivFoodPreview.setImageURI(it)
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
        _binding = FragmentFoodBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.btnFoodCapture.setOnClickListener { ensurePermissionThenCamera() }
        binding.btnFoodPick.setOnClickListener { ensurePermissionThenPick() }
        binding.btnAnalyzeFood.setOnClickListener { analyze() }
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
        val file = File(requireContext().externalCacheDir, "capture_food_${System.currentTimeMillis()}.jpg")
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
            binding.progressFood.visibility = View.VISIBLE
            binding.btnAnalyzeFood.isEnabled = false
            try {
                val profile = withContext(Dispatchers.IO) {
                    AppDatabase.get(requireContext()).userProfileDao().get()
                }
                val analyzer = VisionAnalyzerFactory.create(requireContext())
                    ?: throw IllegalStateException("no analyzer")
                val result = withContext(Dispatchers.IO) {
                    analyzer.analyzeFood(currentBase64, profile)
                }
                showResult(result)
                val rec = AnalysisRecord(
                    type = "food",
                    createdAt = System.currentTimeMillis(),
                    imageBase64 = currentBase64,
                    summary = result.foods.joinToString("、") { it.name },
                    detailJson = Gson().toJson(result)
                )
                withContext(Dispatchers.IO) {
                    AppDatabase.get(requireContext()).analysisRecordDao().insert(rec)
                }
            } catch (e: Exception) {
                Toast.makeText(requireContext(), R.string.error_generic, Toast.LENGTH_LONG).show()
            } finally {
                binding.progressFood.visibility = View.GONE
                binding.btnAnalyzeFood.isEnabled = true
            }
        }
    }

    private fun showResult(r: FoodAnalysis) {
        val sb = StringBuilder()
        if (r.foods.isNotEmpty()) {
            sb.appendLine("识别到的食物：")
            r.foods.forEach { f ->
                sb.appendLine("· ${f.name}（${f.portion}） 约 ${f.caloriesKcal} kcal")
                sb.appendLine("   蛋白 ${f.proteinG}g / 碳水 ${f.carbG}g / 脂肪 ${f.fatG}g")
            }
        }
        sb.appendLine(
            "\n合计：约 ${r.totalCalories} kcal | 蛋白 ${r.totalProteinG}g | 碳水 ${r.totalCarbG}g | 脂肪 ${r.totalFatG}g"
        )
        val suitableText = when (r.suitable) {
            "suitable" -> "适合"
            "moderate" -> "适量即可"
            "unsuitable" -> "不太适合"
            else -> r.suitable
        }
        sb.appendLine("\n是否适合你：$suitableText")
        if (r.reason.isNotBlank()) sb.appendLine("原因：${r.reason}")
        if (r.adjustment.isNotBlank()) sb.appendLine("\n调整建议：${r.adjustment}")
        binding.tvFoodResult.text = sb.toString()
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}
