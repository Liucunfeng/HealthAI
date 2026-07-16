package com.example.healthai.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.healthai.R
import com.example.healthai.data.AppDatabase
import com.example.healthai.data.UserProfile
import com.example.healthai.databinding.FragmentProfileBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        loadProfile()
        binding.btnSaveProfile.setOnClickListener { save() }
    }

    private fun loadProfile() {
        lifecycleScope.launch {
            val p = withContext(Dispatchers.IO) {
                AppDatabase.get(requireContext()).userProfileDao().get()
            }
            p ?: return@launch
            binding.etHeight.setText(if (p.heightCm > 0) p.heightCm.toInt().toString() else "")
            binding.etWeight.setText(if (p.weightKg > 0) p.weightKg.toInt().toString() else "")
            binding.etAge.setText(if (p.age > 0) p.age.toString() else "")
            when (p.gender) {
                "male" -> binding.rgGender.check(R.id.rb_male)
                "female" -> binding.rgGender.check(R.id.rb_female)
            }
            when (p.goal) {
                "lose" -> binding.rgGoal.check(R.id.rb_lose)
                "maintain" -> binding.rgGoal.check(R.id.rb_maintain)
                "gain" -> binding.rgGoal.check(R.id.rb_gain)
            }
            when (p.activity) {
                "low" -> binding.rgActivity.check(R.id.rb_low)
                "mid" -> binding.rgActivity.check(R.id.rb_mid)
                "high" -> binding.rgActivity.check(R.id.rb_high)
            }
        }
    }

    private fun save() {
        val h = binding.etHeight.text.toString().toFloatOrNull() ?: 0f
        val w = binding.etWeight.text.toString().toFloatOrNull() ?: 0f
        val age = binding.etAge.text.toString().toIntOrNull() ?: 0
        val gender = when (binding.rgGender.checkedRadioButtonId) {
            R.id.rb_male -> "male"
            R.id.rb_female -> "female"
            else -> ""
        }
        val goal = when (binding.rgGoal.checkedRadioButtonId) {
            R.id.rb_lose -> "lose"
            R.id.rb_maintain -> "maintain"
            R.id.rb_gain -> "gain"
            else -> ""
        }
        val activity = when (binding.rgActivity.checkedRadioButtonId) {
            R.id.rb_low -> "low"
            R.id.rb_mid -> "mid"
            R.id.rb_high -> "high"
            else -> ""
        }
        val profile = UserProfile(
            heightCm = h, weightKg = w, age = age,
            gender = gender, goal = goal, activity = activity
        )
        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                AppDatabase.get(requireContext()).userProfileDao().upsert(profile)
            }
            Toast.makeText(requireContext(), R.string.profile_saved, Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}
