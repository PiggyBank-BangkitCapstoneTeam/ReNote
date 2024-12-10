package com.piggybank.renote.data.response

import com.google.gson.annotations.SerializedName

data class UploadFotoResponse(

	@field:SerializedName("data")
	val data: Data? = null,

	@field:SerializedName("status")
	val status: Int? = null
)

data class Data(

	@field:SerializedName("note_id")
	val noteId: String? = null,

	@field:SerializedName("id")
	val id: String? = null,

	@field:SerializedName("url")
	val url: String? = null
)
