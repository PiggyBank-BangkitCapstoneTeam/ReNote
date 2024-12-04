package com.piggybank.renote.data.response

import com.google.gson.annotations.SerializedName

data class NoteandRekeningResponse(

	@field:SerializedName("item")
	val item: List<ItemItem?>? = null,

	@field:SerializedName("auth")
	val auth: Auth? = null,

	@field:SerializedName("variable")
	val variable: List<VariableItem?>? = null,

	@field:SerializedName("event")
	val event: List<EventItem?>? = null,

	@field:SerializedName("info")
	val info: Info? = null
)

data class Auth(

	@field:SerializedName("bearer")
	val bearer: List<BearerItem?>? = null,

	@field:SerializedName("type")
	val type: String? = null
)

data class Script(

	@field:SerializedName("type")
	val type: String? = null,

	@field:SerializedName("packages")
	val packages: Packages? = null,

	@field:SerializedName("exec")
	val exec: List<String?>? = null
)

data class Info(

	@field:SerializedName("schema")
	val schema: String? = null,

	@field:SerializedName("name")
	val name: String? = null,

	@field:SerializedName("description")
	val description: String? = null,

	@field:SerializedName("_exporter_id")
	val exporterId: String? = null,

	@field:SerializedName("_postman_id")
	val postmanId: String? = null
)

data class Url(

	@field:SerializedName("path")
	val path: List<String?>? = null,

	@field:SerializedName("host")
	val host: List<String?>? = null,

	@field:SerializedName("raw")
	val raw: String? = null,

	@field:SerializedName("variable")
	val variable: List<VariableItem?>? = null
)

data class Options(

	@field:SerializedName("raw")
	val raw: Raw? = null
)

data class HeaderItem(

	@field:SerializedName("value")
	val value: String? = null,

	@field:SerializedName("key")
	val key: String? = null
)

data class BearerItem(

	@field:SerializedName("type")
	val type: String? = null,

	@field:SerializedName("value")
	val value: String? = null,

	@field:SerializedName("key")
	val key: String? = null
)

data class ItemItem(

	@field:SerializedName("request")
	val request: Request? = null,

	@field:SerializedName("response")
	val response: List<ResponseItem?>? = null,

	@field:SerializedName("name")
	val name: String? = null,

	@field:SerializedName("item")
	val item: List<ItemItem?>? = null,

	@field:SerializedName("description")
	val description: String? = null
)

data class EventItem(

	@field:SerializedName("listen")
	val listen: String? = null,

	@field:SerializedName("script")
	val script: Script? = null
)

data class Packages(
	val any: Any? = null
)

data class Body(

	@field:SerializedName("mode")
	val mode: String? = null,

	@field:SerializedName("options")
	val options: Options? = null,

	@field:SerializedName("raw")
	val raw: String? = null
)

data class Request(

	@field:SerializedName("method")
	val method: String? = null,

	@field:SerializedName("header")
	val header: List<Any?>? = null,

	@field:SerializedName("url")
	val url: Url? = null,

	@field:SerializedName("body")
	val body: Body? = null
)

data class OriginalRequest(

	@field:SerializedName("method")
	val method: String? = null,

	@field:SerializedName("header")
	val header: List<Any?>? = null,

	@field:SerializedName("url")
	val url: Url? = null,

	@field:SerializedName("body")
	val body: Body? = null
)

data class Raw(

	@field:SerializedName("language")
	val language: String? = null
)

data class VariableItem(

	@field:SerializedName("description")
	val description: String? = null,

	@field:SerializedName("value")
	val value: String? = null,

	@field:SerializedName("key")
	val key: String? = null,

	@field:SerializedName("type")
	val type: String? = null
)

data class ResponseItem(

	@field:SerializedName("originalRequest")
	val originalRequest: OriginalRequest? = null,

	@field:SerializedName("_postman_previewlanguage")
	val postmanPreviewlanguage: String? = null,

	@field:SerializedName("code")
	val code: Int? = null,

	@field:SerializedName("cookie")
	val cookie: List<Any?>? = null,

	@field:SerializedName("name")
	val name: String? = null,

	@field:SerializedName("header")
	val header: List<HeaderItem?>? = null,

	@field:SerializedName("body")
	val body: String? = null,

	@field:SerializedName("status")
	val status: String? = null
)
