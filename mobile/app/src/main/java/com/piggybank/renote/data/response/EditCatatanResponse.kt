package com.piggybank.renote.data.response

import com.google.gson.annotations.SerializedName

data class EditCatatanResponse(

	@field:SerializedName("message")
	val message: String? = null,

	@field:SerializedName("status")
	val status: Int? = null
)
