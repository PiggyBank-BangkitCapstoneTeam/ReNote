package com.piggybank.renotes.ui.rekening

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Rekening(
    val name: String,
    val uang: Int,
) : Parcelable
