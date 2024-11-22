package com.piggybank.renote.ui.main

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager2.widget.ViewPager2
import com.piggybank.renote.R
import com.piggybank.renote.ui.welcome.WelcomeAdapter

class WelcomeActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()
        setContentView(R.layout.activity_welcome)

        val viewPager: ViewPager2 = findViewById(R.id.viewPager)

        viewPager.adapter = WelcomeAdapter(this)
    }
}
