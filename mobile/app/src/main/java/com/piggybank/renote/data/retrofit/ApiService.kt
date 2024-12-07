package com.piggybank.renotes.data.retrofit

import com.piggybank.renote.data.response.GetAllNoteResponse
import com.piggybank.renotes.data.response.LoginResponse
import retrofit2.Call
import retrofit2.http.GET

interface ApiService {
    @GET("/")
    fun getLoginMessage(): Call<LoginResponse>

    @GET("/kumpulan_note")
    fun getAllNotes(): Call<GetAllNoteResponse>
}