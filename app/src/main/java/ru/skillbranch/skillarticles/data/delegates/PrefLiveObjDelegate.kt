package ru.skillbranch.skillarticles.data.delegates

import android.content.SharedPreferences
import androidx.lifecycle.LiveData
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import ru.skillbranch.skillarticles.data.models.User
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

class PrefLiveObjDelegate<T>(
    private val fieldKey: String,
    private val defaultValue: JsonAdapter<T>,
    private val preferences: SharedPreferences
) : ReadOnlyProperty<Any?, LiveData<T>> {

    private var storedValue: LiveData<Any>? = null

    override fun getValue(thisRef: Any?, property: KProperty<*>): LiveData<T> {
        if (storedValue == null) {
            storedValue = SharedPreferencesLiveData(preferences, fieldKey, defaultValue.toJson(T))
        }
        return storedValue!!
    }
}