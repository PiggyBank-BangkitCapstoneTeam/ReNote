package com.piggybank.renote.data.retrofit

import com.piggybank.renote.data.response.LoginResponse
import retrofit2.Call
import retrofit2.http.GET

interface ApiService {
    @GET("/")
    fun getLoginMessage(): Call<LoginResponse>
}