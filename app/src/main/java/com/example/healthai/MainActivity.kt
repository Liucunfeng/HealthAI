package com.example.healthai

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.example.healthai.databinding.ActivityMainBinding
import com.example.healthai.ui.BodyAnalysisFragment
import com.example.healthai.ui.FoodAnalysisFragment
import com.example.healthai.ui.HistoryFragment
import com.example.healthai.ui.ProfileFragment
import com.example.healthai.ui.SettingsFragment

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (savedInstanceState == null) {
            switchTo(BodyAnalysisFragment())
            binding.bottomNav.selectedItemId = R.id.nav_body
        }

        binding.bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_body -> switchTo(BodyAnalysisFragment())
                R.id.nav_food -> switchTo(FoodAnalysisFragment())
                R.id.nav_profile -> switchTo(ProfileFragment())
                R.id.nav_settings -> switchTo(SettingsFragment())
                R.id.nav_history -> switchTo(HistoryFragment())
                else -> return@setOnItemSelectedListener false
            }
            true
        }
    }

    private fun switchTo(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.container, fragment)
            .commit()
    }
}
