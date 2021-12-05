package ru.skillbranch.skillarticles.data.delegates

import android.content.SharedPreferences
import androidx.lifecycle.LiveData
import com.squareup.moshi.JsonAdapter
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

class PrefLiveObjDelegate<T>(
    private val fieldKey: String,
    private val defaultValue: JsonAdapter<T>,
    private val preferences: SharedPreferences
) : ReadOnlyProperty<Any?, LiveData<T>> {

    private var storedValue: LiveData<T>? = null

    override fun getValue(thisRef: Any?, property: KProperty<*>): LiveData<T> {
        if (storedValue == null) {
            storedValue = SharedPreferencesObjLiveData(preferences, fieldKey, defaultValue)
        }
        return storedValue!!
    }
}

internal class SharedPreferencesObjLiveData<T>(
    var sharedPrefs: SharedPreferences,
    var key: String,
    var defValue: JsonAdapter<T>
) : LiveData<T>() {
    private val preferencesChangeListener =
        SharedPreferences.OnSharedPreferenceChangeListener{ _, shKey ->
            if (shKey == key) {
                value = readValue(defValue)
            }
        }

    override fun onActive() {
        super.onActive()
        value = readValue(defValue)
        sharedPrefs.registerOnSharedPreferenceChangeListener(preferencesChangeListener)
    }

    override fun onInactive() {
        sharedPrefs.unregisterOnSharedPreferenceChangeListener(preferencesChangeListener)
        super.onInactive()
    }

    @Suppress("UNCHECKED_CAST")
    private fun readValue(defaultValue: JsonAdapter<T>): T? {
        return sharedPrefs.getString(key, null)
            ?.let { defaultValue.fromJson(it) }
    }
}