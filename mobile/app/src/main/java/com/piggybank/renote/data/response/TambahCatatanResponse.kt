package com.piggybank.renote.data.response

import com.google.gson.annotations.SerializedName

data class TambahCatatanResponse(

	@field:SerializedName("nominal")
	val nominal: Int? = null,

	@field:SerializedName("kategori")
	val kategori: String? = null,

	@field:SerializedName("deskripsi")
	val deskripsi: String? = null,

	@field:SerializedName("tanggal")
	val tanggal: String? = null
)
