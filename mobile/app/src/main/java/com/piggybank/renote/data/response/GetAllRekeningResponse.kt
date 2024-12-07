package com.piggybank.renotes.data.response

import com.google.gson.annotations.SerializedName

data class GetAllRekeningResponse(

	@field:SerializedName("data")
	val data: List<DataItem?>? = null,

	@field:SerializedName("status")
	val status: Int? = null
)

data class DataItem(

	@field:SerializedName("uang")
	val uang: Int? = null,

	@field:SerializedName("name")
	val name: String? = null,

	@field:SerializedName("id")
	val id: String? = null
)
