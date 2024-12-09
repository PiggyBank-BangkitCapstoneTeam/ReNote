package com.piggybank.renotes.data.retrofit

import com.piggybank.renote.data.response.GetAllNoteResponse
import com.piggybank.renote.data.response.HapusCatatanResponse
import com.piggybank.renote.data.response.TambahCatatanResponse
import com.piggybank.renotes.data.response.LoginResponse
import com.piggybank.renotes.ui.catatan.Catatan
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface ApiService {
    @GET("/")
    fun getLoginMessage(): Call<LoginResponse>

    @GET("/kumpulan_note")
    fun getAllNotes(): Call<GetAllNoteResponse>

    @POST("/note")
    fun addNote(@Body request: Catatan): Call<TambahCatatanResponse>

    @DELETE("/note/{id}")
    fun deleteNote(@Path("id") noteId: String): Call<HapusCatatanResponse>
}