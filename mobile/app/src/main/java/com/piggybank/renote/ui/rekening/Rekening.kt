package com.piggybank.renote.ui.rekening

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
data class Rekening(
    val name: String,
    val uang: Long,
) : Parcelable
