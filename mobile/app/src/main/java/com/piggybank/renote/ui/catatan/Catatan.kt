package com.piggybank.renotes.ui.catatan

data class Catatan(
    val id: String,
    val kategori: String,
    val nominal: Int,
    val deskripsi: String,
    val tanggal: String
)
