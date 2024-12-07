package com.piggybank.renotes.data.response

import com.google.gson.annotations.SerializedName

data class LoginResponse(

	@field:SerializedName("message")
	val message: String? = null,

	@field:SerializedName("status")
	val status: Int? = null
)
