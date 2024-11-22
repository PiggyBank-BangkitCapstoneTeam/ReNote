package com.piggybank.renote.ui.main

import android.os.Bundle
import android.view.View
import android.view.animation.ScaleAnimation
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.findNavController
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.piggybank.renote.R
import com.piggybank.renote.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val navView: BottomNavigationView = binding.navView
        val navController = findNavController(R.id.nav_host_fragment_activity_main)

        navView.setupWithNavController(navController)

        supportActionBar?.hide()

        navView.setOnItemReselectedListener { item ->
            val view = navView.findViewById<View>(item.itemId)
            val scaleAnimation = ScaleAnimation(
                0.9f, 1.1f, 0.9f, 1.1f,
                ScaleAnimation.RELATIVE_TO_SELF, 0.5f,
                ScaleAnimation.RELATIVE_TO_SELF, 0.5f
            ).apply {
                duration = 150
                fillAfter = true
            }
            view.startAnimation(scaleAnimation)
        }
    }
}
