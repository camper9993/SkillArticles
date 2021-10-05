package ru.skillbranch.skillarticles.data.local

import android.content.SharedPreferences
import androidx.lifecycle.*
import androidx.preference.PreferenceManager
import ru.skillbranch.skillarticles.App
import ru.skillbranch.skillarticles.JsonConverter.moshi
import ru.skillbranch.skillarticles.data.delegates.PrefDelegate
import ru.skillbranch.skillarticles.data.delegates.PrefLiveDelegate
import ru.skillbranch.skillarticles.data.delegates.PrefLiveObjDelegate
import ru.skillbranch.skillarticles.data.delegates.PrefObjDelegate
import ru.skillbranch.skillarticles.data.models.AppSettings
import ru.skillbranch.skillarticles.data.models.User

object PrefManager {
    internal val preferences : SharedPreferences by lazy {
        PreferenceManager.getDefaultSharedPreferences(App.applicationContext())
    }

    var isDarkMode by PrefDelegate(false)
    var isBigText by PrefDelegate(false)
    var accessToken by PrefDelegate("")
    var refreshToken by PrefDelegate("")
    var profile: User? by PrefObjDelegate(moshi.adapter(User::class.java))

    fun clearAll() {
        preferences.edit().clear().apply()
    }

    val appSettings = MediatorLiveData<AppSettings>().apply {
        val isDarkModeLive: LiveData<Boolean> by PrefLiveDelegate("isDarkMode", false, preferences)
        val isBigTextLive: LiveData<Boolean> by PrefLiveDelegate("isBigText", false, preferences)
        value = AppSettings()

        addSource(isDarkModeLive) {
            value = value!!.copy(isDarkMode = it)
        }

        addSource(isBigTextLive) {
            value = value!!.copy(isBigText = it)
        }
    }.distinctUntilChanged()

    val profileAlive: LiveData<User?> by PrefLiveObjDelegate(
        "profile",
        moshi.adapter(User::class.java),
        preferences
    )

    val isAuthLive: LiveData<Boolean> by lazy {
        val token by PrefLiveDelegate("accessToken", "", preferences)
        token.map { it.isNotEmpty() }
    }
}