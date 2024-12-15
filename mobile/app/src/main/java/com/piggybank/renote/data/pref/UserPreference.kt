package com.piggybank.renotes.data.pref

import android.content.Context

class UserPreference(context: Context) {
    private val preferences = context.getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)

    fun saveToken(token: String) {
        with(preferences.edit()) {
            putString("userToken", token)
            apply()
        }
    }

    fun getToken(): String? {
        return preferences.getString("userToken", null)
    }

    fun clearToken() {
        with(preferences.edit()) {
            remove("userToken")
            apply()
        }
    }
}
