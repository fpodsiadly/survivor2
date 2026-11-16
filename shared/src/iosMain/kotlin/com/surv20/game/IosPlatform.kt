package com.surv20.game

import androidx.compose.ui.window.ComposeUIViewController
import com.surv20.game.core.KeyValueStore
import com.surv20.game.ui.GameTheme
import platform.Foundation.NSBundle
import platform.Foundation.NSProcessInfo
import platform.Foundation.NSUserDefaults
import platform.UIKit.UIViewController

// iOS entry (called from iosApp/iOSApp.swift): the same game as on Android.
fun MainViewController(): UIViewController = ComposeUIViewController {
    GameTheme {
        GameApp(IosPlatform)
    }
}

// iOS: save in NSUserDefaults.
object IosPlatform : GamePlatform {
    override val version: String
        get() {
            val info = NSBundle.mainBundle
            val name = info.objectForInfoDictionaryKey("CFBundleShortVersionString") as? String ?: "?"
            val build = info.objectForInfoDictionaryKey("CFBundleVersion") as? String ?: "?"
            return "$name · iOS · build $build"
        }

    override val store: KeyValueStore = UserDefaultsStore(NSUserDefaults.standardUserDefaults)

    override fun elapsedMillis(): Long = (NSProcessInfo.processInfo.systemUptime * 1000.0).toLong()
}

class UserDefaultsStore(private val defaults: NSUserDefaults) : KeyValueStore {
    override fun getString(key: String): String? = defaults.stringForKey(key)

    override fun putString(key: String, value: String?) {
        if (value == null) defaults.removeObjectForKey(key) else defaults.setObject(value, key)
    }
}
