package fasolato.click.t9launcher

import android.content.Context

class OptionsRepository(context: Context) {
    private val prefs = context.getSharedPreferences("t9launcher_options", Context.MODE_PRIVATE)

    var showRecentlyInstalled: Boolean
        get() = prefs.getBoolean("show_recently_installed", true)
        set(value) { prefs.edit().putBoolean("show_recently_installed", value).apply() }

    var recentlyInstalledMinutes: Int
        get() = prefs.getInt("recently_installed_minutes", 10)
        set(value) { prefs.edit().putInt("recently_installed_minutes", value).apply() }

    var showRecentlyLaunched: Boolean
        get() = prefs.getBoolean("show_recently_launched", true)
        set(value) { prefs.edit().putBoolean("show_recently_launched", value).apply() }
}
