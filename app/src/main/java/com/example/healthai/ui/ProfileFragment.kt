package com.example.healthai.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.healthai.R
import com.example.healthai.data.AppDatabase
import com.example.healthai.data.AppPreferences
import com.example.healthai.data.UserProfile
import com.example.healthai.databinding.DialogProfileEditBinding
import com.example.healthai.databinding.FragmentProfileBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 多人档案管理页（R2）。
 * - 列表展示全部档案，空名显示"未命名档案"；
 * - 新增 / 编辑（最多 10 人，达上限禁用并 Toast）；
 * - 「设为当前」写入 activeProfileId；删除当前激活档案后回退为 0（下次默认首个建档人）。
 */
class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!
    private var adapter: ProfileAdapter? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.rvProfiles.layoutManager = LinearLayoutManager(requireContext())
        adapter = ProfileAdapter(
            items = emptyList(),
            activeId = 0L,
            onSetCurrent = ::onSetCurrent,
            onEdit = ::onEdit,
            onDelete = ::onDelete
        )
        binding.rvProfiles.adapter = adapter
        binding.fabAddProfile.setOnClickListener { onAdd() }
        loadProfiles()
    }

    /** 载入档案列表，并校正 activeProfileId（不存在则回退首个建档人） */
    private fun loadProfiles() {
        lifecycleScope.launch {
            val (list, activeId) = withContext(Dispatchers.IO) {
                val dao = AppDatabase.get(requireContext()).userProfileDao()
                val l = dao.getAll()
                var aid = AppPreferences.getActiveProfileId(requireContext())
                if (aid != 0L && l.none { it.id == aid }) aid = 0L
                if (aid == 0L && l.isNotEmpty()) {
                    AppPreferences.setActiveProfileId(requireContext(), l.first().id)
                    aid = l.first().id
                }
                l to aid
            }
            adapter?.submit(list, activeId)
            // 10 人上限：禁用新增按钮，保持 Toast 提示
            binding.fabAddProfile.isEnabled = list.size < 10
        }
    }

    private fun onAdd() {
        lifecycleScope.launch {
            val count = withContext(Dispatchers.IO) {
                AppDatabase.get(requireContext()).userProfileDao().count()
            }
            if (count >= 10) {
                Toast.makeText(requireContext(), R.string.profile_count_limit, Toast.LENGTH_SHORT).show()
                binding.fabAddProfile.isEnabled = false
                return@launch
            }
            openDialog(null)
        }
    }

    private fun onSetCurrent(p: UserProfile) {
        AppPreferences.setActiveProfileId(requireContext(), p.id)
        Toast.makeText(requireContext(), R.string.profile_set_current_toast, Toast.LENGTH_SHORT).show()
        loadProfiles()
    }

    private fun onEdit(p: UserProfile) {
        openDialog(p)
    }

    private fun onDelete(p: UserProfile) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.profile_delete_title)
            .setMessage(R.string.profile_delete_msg)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                lifecycleScope.launch {
                    withContext(Dispatchers.IO) {
                        val dao = AppDatabase.get(requireContext()).userProfileDao()
                        dao.delete(p)
                        if (AppPreferences.getActiveProfileId(requireContext()) == p.id) {
                            // 删的是当前激活档案 → 回退为 0，下次默认首个建档人
                            AppPreferences.setActiveProfileId(requireContext(), 0L)
                        }
                    }
                    loadProfiles()
                }
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    /** 新增（profile=null）或编辑（profile!=null）档案对话框 */
    private fun openDialog(profile: UserProfile?) {
        val dialogBinding = DialogProfileEditBinding.inflate(layoutInflater)
        profile?.let { p ->
            dialogBinding.etName.setText(p.name)
            if (p.heightCm > 0) dialogBinding.etHeight.setText(p.heightCm.toInt().toString())
            if (p.weightKg > 0) dialogBinding.etWeight.setText(p.weightKg.toInt().toString())
            if (p.age > 0) dialogBinding.etAge.setText(p.age.toString())
            when (p.gender) {
                "male" -> dialogBinding.rgGender.check(R.id.rb_male)
                "female" -> dialogBinding.rgGender.check(R.id.rb_female)
            }
            when (p.goal) {
                "lose" -> dialogBinding.rgGoal.check(R.id.rb_lose)
                "maintain" -> dialogBinding.rgGoal.check(R.id.rb_maintain)
                "gain" -> dialogBinding.rgGoal.check(R.id.rb_gain)
            }
            when (p.activity) {
                "low" -> dialogBinding.rgActivity.check(R.id.rb_low)
                "mid" -> dialogBinding.rgActivity.check(R.id.rb_mid)
                "high" -> dialogBinding.rgActivity.check(R.id.rb_high)
            }
        }
        val title = if (profile == null) R.string.dialog_profile_title_new else R.string.dialog_profile_title_edit
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(title)
            .setView(dialogBinding.root)
            .setPositiveButton(android.R.string.ok) { _, _ -> saveFromDialog(dialogBinding, profile) }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun saveFromDialog(b: DialogProfileEditBinding, existing: UserProfile?) {
        val name = b.etName.text.toString().trim()
        val h = b.etHeight.text.toString().toFloatOrNull() ?: 0f
        val w = b.etWeight.text.toString().toFloatOrNull() ?: 0f
        val age = b.etAge.text.toString().toIntOrNull() ?: 0
        val gender = when (b.rgGender.checkedRadioButtonId) {
            R.id.rb_male -> "male"
            R.id.rb_female -> "female"
            else -> ""
        }
        val goal = when (b.rgGoal.checkedRadioButtonId) {
            R.id.rb_lose -> "lose"
            R.id.rb_maintain -> "maintain"
            R.id.rb_gain -> "gain"
            else -> ""
        }
        val activity = when (b.rgActivity.checkedRadioButtonId) {
            R.id.rb_low -> "low"
            R.id.rb_mid -> "mid"
            R.id.rb_high -> "high"
            else -> ""
        }
        val entity = if (existing == null) {
            UserProfile(name = name, heightCm = h, weightKg = w, age = age, gender = gender, goal = goal, activity = activity)
        } else {
            existing.copy(name = name, heightCm = h, weightKg = w, age = age, gender = gender, goal = goal, activity = activity)
        }
        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                val dao = AppDatabase.get(requireContext()).userProfileDao()
                val id = if (existing == null) {
                    dao.insert(entity)
                } else {
                    dao.update(entity)
                    existing.id
                }
                if (AppPreferences.getActiveProfileId(requireContext()) == 0L) {
                    // 首次建档 / 当前无激活档案时，自动设为当前
                    AppPreferences.setActiveProfileId(requireContext(), id)
                }
            }
            Toast.makeText(requireContext(), R.string.profile_saved, Toast.LENGTH_SHORT).show()
            loadProfiles()
        }
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}
