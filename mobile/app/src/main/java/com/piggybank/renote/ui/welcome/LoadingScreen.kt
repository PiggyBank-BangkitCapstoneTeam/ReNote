package com.piggybank.renote.ui.welcome

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import com.piggybank.renote.R

class LoadingScreen(context: Context) : Dialog(context) {
    init {
        setCancelable(false)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.screen_loading)
    }
}