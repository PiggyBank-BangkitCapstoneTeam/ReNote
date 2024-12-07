package com.piggybank.renotes.data.retrofit

import com.piggybank.renotes.data.response.LoginResponse
import retrofit2.Call
import retrofit2.http.GET

interface ApiService {
    @GET("/")
    fun getLoginMessage(): Call<LoginResponse>
}