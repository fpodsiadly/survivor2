package com.surv20.game.core

// Minimal key-value storage implemented by SharedPreferences (Android) and NSUserDefaults (iOS).
interface KeyValueStore {
    fun getString(key: String): String?
    fun putString(key: String, value: String?)
}

// In-memory store for tests and previews.
class MemoryStore : KeyValueStore {
    val values = mutableMapOf<String, String>()
    override fun getString(key: String): String? = values[key]
    override fun putString(key: String, value: String?) {
        if (value == null) values.remove(key) else values[key] = value
    }
}
