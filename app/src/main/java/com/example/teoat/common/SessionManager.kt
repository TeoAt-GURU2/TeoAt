package com.example.teoat.common

import android.content.Context

class SessionManager(context: Context) {

    private val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    fun setLoggedIn(userId: String) {
        prefs.edit()
            .putBoolean(KEY_LOGGED_IN, true)
            .putString(KEY_USER_ID, userId)
            .apply()
    }

    fun isLoggedIn(): Boolean = prefs.getBoolean(KEY_LOGGED_IN, false)

    fun getUserId(): String? = prefs.getString(KEY_USER_ID, null)

    fun logout() {
        prefs.edit()
            .putBoolean(KEY_LOGGED_IN, false)
            .remove(KEY_USER_ID)
            .apply()
    }

    companion object {
        private const val PREF_NAME = "teoat_session"
        private const val KEY_LOGGED_IN = "logged_in"
        private const val KEY_USER_ID = "user_id"
    }
}
