package com.surv20.game

import android.content.Context
import android.content.SharedPreferences
import android.content.pm.ApplicationInfo
import android.os.SystemClock
import com.surv20.game.core.KeyValueStore

// Android: save in SharedPreferences.
class AndroidPlatform(
    context: Context,
    override val version: String,
) : GamePlatform {
    override val store: KeyValueStore = SharedPrefsStore(context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE))

    override fun elapsedMillis(): Long = SystemClock.elapsedRealtime()

    override val isDebugBuild: Boolean = (context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0

    companion object {
        const val PREFS_NAME = "surv20"
    }
}

class SharedPrefsStore(private val prefs: SharedPreferences) : KeyValueStore {
    override fun getString(key: String): String? = prefs.getString(key, null)

    // commit() writes synchronously, so a save is on disk before the call returns (SPEC W42).
    override fun putString(key: String, value: String?) {
        prefs.edit().apply { if (value == null) remove(key) else putString(key, value) }.commit()
    }
}
