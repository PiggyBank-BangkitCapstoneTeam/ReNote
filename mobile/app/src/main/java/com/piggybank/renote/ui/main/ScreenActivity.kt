package com.piggybank.renote.ui.main

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import androidx.core.animation.addListener
import com.piggybank.renote.databinding.ActivityScreenBinding

class ScreenActivity : AppCompatActivity() {

    private lateinit var binding: ActivityScreenBinding
    private var hasAnimationStarted = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityScreenBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.logo.alpha = 0f
        binding.textSplashApk.alpha = 0f

        binding.root.viewTreeObserver.addOnPreDrawListener {
            if (!hasAnimationStarted) {
                Handler(Looper.getMainLooper()).postDelayed({
                    startEntryAnimation()
                }, 100)
                hasAnimationStarted = true
            }
            true
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)

        if (hasFocus) {
            Handler(Looper.getMainLooper()).postDelayed({
                startExitAnimation()
            }, 5000L)
        }
    }

    private fun startEntryAnimation() {

        val fadeInLogo = ObjectAnimator.ofFloat(binding.logo, "alpha", 0f, 1f).apply {
            duration = 1000
        }
        val scaleXLogo = ObjectAnimator.ofFloat(binding.logo, "scaleX", 0.5f, 1f).apply {
            duration = 1000
        }
        val scaleYLogo = ObjectAnimator.ofFloat(binding.logo, "scaleY", 0.5f, 1f).apply {
            duration = 1000
        }

        val fadeInText = ObjectAnimator.ofFloat(binding.textSplashApk, "alpha", 0f, 1f).apply {
            duration = 1000
        }
        val scaleXText = ObjectAnimator.ofFloat(binding.textSplashApk, "scaleX", 0.5f, 1f).apply {
            duration = 1000
        }
        val scaleYText = ObjectAnimator.ofFloat(binding.textSplashApk, "scaleY", 0.5f, 1f).apply {
            duration = 1000
        }

        AnimatorSet().apply {
            playTogether(fadeInLogo, scaleXLogo, scaleYLogo, fadeInText, scaleXText, scaleYText)
            start()
        }
    }

    private fun startExitAnimation() {
        val fadeOutLogo = ObjectAnimator.ofFloat(binding.logo, "alpha", 1f, 0f).apply {
            duration = 1000
        }
        val fadeOutText = ObjectAnimator.ofFloat(binding.textSplashApk, "alpha", 1f, 0f).apply {
            duration = 1000
        }

        AnimatorSet().apply {
            playTogether(fadeOutLogo, fadeOutText)
            start()
            addListener(onEnd = {
                moveMainActivity()
            })
        }
    }

    private fun moveMainActivity() {
        Intent(this, WelcomeActivity::class.java).also {
            startActivity(it)
            finish()
        }
    }
}
