package com.piggybank.renotes.data.retrofit

import com.piggybank.renote.data.response.GetAllNoteResponse
import com.piggybank.renote.data.response.TambahCatatanResponse
import com.piggybank.renotes.data.response.LoginResponse
import com.piggybank.renotes.ui.catatan.Catatan
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface ApiService {
    @GET("/")
    fun getLoginMessage(): Call<LoginResponse>

    @GET("/kumpulan_note")
    fun getAllNotes(): Call<GetAllNoteResponse>

    @POST("/note")
    fun addNoteToServer(@Body request: Catatan): Call<TambahCatatanResponse>
}