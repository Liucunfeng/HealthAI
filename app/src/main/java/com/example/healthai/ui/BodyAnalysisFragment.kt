package com.example.healthai.ui

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.healthai.R
import com.example.healthai.data.AnalysisRecord
import com.example.healthai.data.AppDatabase
import com.example.healthai.data.AppPreferences
import com.example.healthai.data.UserProfile
import com.example.healthai.databinding.FragmentBodyBinding
import com.example.healthai.util.ImageUtils
import com.example.healthai.util.ResultFormatter
import com.example.healthai.vision.BodyAnalysis
import com.example.healthai.vision.VisionAnalyzerFactory
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/**
 * 身材分析页（R1 多图 + R2 选人）。
 * - 最多 4 张 4:3 缩略图，可单独删除；
 * - 选人下拉（含"通用建议（不指定）"），默认跟随 activeProfileId / 首个建档人；
 * - 分析成功后将可读中文写入 AnalysisRecord.displayText 并随首图入库。
 */
class BodyAnalysisFragment : Fragment() {

    private var _binding: FragmentBodyBinding? = null
    private val binding get() = _binding!!

    private val selectedUris: MutableList<Uri> = mutableListOf()
    private val selectedBase64: MutableList<String> = mutableListOf()
    private var pendingUri: Uri? = null
    private var selectedProfileId: Long = 0L
    private var profileOptions: List<ProfileOption> = emptyList()
    private var imageAdapter: ImageThumbAdapter? = null

    private val takePhoto = registerForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.TakePicture()
    ) { ok -> if (ok) pendingUri?.let { addImage(it) } }

    private val pickImage = registerForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.GetContent()
    ) { uri -> uri?.let { addImage(it) } }

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
        binding.rvImages.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        imageAdapter = ImageThumbAdapter(
            uris = selectedUris,
            onAdd = { ensurePermissionThenPick() },
            onRemove = { removeImageAt(it) }
        )
        binding.rvImages.adapter = imageAdapter

        binding.btnCapture.setOnClickListener { ensurePermissionThenCamera() }
        binding.btnPick.setOnClickListener { ensurePermissionThenPick() }
        binding.btnAnalyze.setOnClickListener { analyze() }

        loadProfilesAndSetupDropdown()
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

    private fun addImage(uri: Uri) {
        if (selectedUris.size >= 4) {
            Toast.makeText(requireContext(), R.string.max_images, Toast.LENGTH_SHORT).show()
            return
        }
        val b64 = ImageUtils.uriToCompressedBase64(requireContext(), uri)
        if (b64.isBlank()) {
            Toast.makeText(requireContext(), R.string.error_image_load, Toast.LENGTH_SHORT).show()
            return
        }
        selectedUris.add(uri)
        selectedBase64.add(b64)
        imageAdapter?.notifyDataSetChanged()
    }

    private fun removeImageAt(index: Int) {
        if (index < 0 || index >= selectedUris.size) return
        selectedUris.removeAt(index)
        selectedBase64.removeAt(index)
        imageAdapter?.notifyDataSetChanged()
    }

    /** 载入档案并初始化选人下拉：默认跟随 activeProfileId，否则默认首个建档人，否则通用建议 */
    private fun loadProfilesAndSetupDropdown() {
        lifecycleScope.launch {
            val (profiles, activeId) = withContext(Dispatchers.IO) {
                val dao = AppDatabase.get(requireContext()).userProfileDao()
                val list = dao.getAll()
                list to AppPreferences.getActiveProfileId(requireContext())
            }
            val activeExists = profiles.any { it.id == activeId }
            val effectiveId = if (activeExists) activeId
            else if (profiles.isNotEmpty()) {
                AppPreferences.setActiveProfileId(requireContext(), profiles.first().id)
                profiles.first().id
            } else 0L
            selectedProfileId = effectiveId

            val options = mutableListOf(ProfileOption(0L, getString(R.string.profile_generic)))
            options.addAll(profiles.map { ProfileOption(it.id, displayName(it)) })
            profileOptions = options

            val adapter = ArrayAdapter(
                requireContext(),
                android.R.layout.simple_dropdown_item_1line,
                options.map { it.name }.toTypedArray()
            )
            binding.autoProfile.setAdapter(adapter)
            val selectedName = options.firstOrNull { it.id == effectiveId }?.name
                ?: getString(R.string.profile_generic)
            binding.autoProfile.setText(selectedName, false)
            binding.autoProfile.setOnItemClickListener { _, _, pos, _ ->
                selectedProfileId = profileOptions.getOrNull(pos)?.id ?: 0L
            }
        }
    }

    private fun displayName(p: UserProfile): String =
        if (p.name.isBlank()) getString(R.string.unnamed_profile) else p.name

    private fun analyze() {
        if (selectedBase64.isEmpty()) {
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
                val profile = if (selectedProfileId == 0L) null else withContext(Dispatchers.IO) {
                    AppDatabase.get(requireContext()).userProfileDao().getById(selectedProfileId)
                }
                val analyzer = VisionAnalyzerFactory.create(requireContext())
                    ?: throw IllegalStateException("no analyzer")
                val result = withContext(Dispatchers.IO) {
                    analyzer.analyzeBody(selectedBase64, profile)
                }
                val display = ResultFormatter.formatBody(result)
                binding.tvResult.text = display
                val profileName = profile?.name ?: ""
                val rec = AnalysisRecord(
                    type = "body",
                    createdAt = System.currentTimeMillis(),
                    imageBase64 = selectedBase64.first(),
                    imageListJson = Gson().toJson(selectedBase64),
                    summary = result.overall,
                    detailJson = Gson().toJson(result),
                    displayText = display,
                    profileName = profileName
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

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}
