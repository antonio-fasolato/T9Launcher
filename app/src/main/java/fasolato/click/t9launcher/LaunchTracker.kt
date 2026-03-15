package fasolato.click.t9launcher

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

class LaunchTracker(context: Context) {

    private val prefs = context.getSharedPreferences("launch_history", Context.MODE_PRIVATE)
    private val tenDaysMs = 10L * 24 * 60 * 60 * 1000

    fun recordLaunch(packageName: String) {
        val history = loadHistory()
        val timestamps = history.getOrPut(packageName) { mutableListOf() }
        timestamps.add(System.currentTimeMillis())
        saveHistory(history)
    }

    fun getLaunchCount(packageName: String): Int {
        val cutoff = System.currentTimeMillis() - tenDaysMs
        return loadHistory()[packageName]?.count { it >= cutoff } ?: 0
    }

    private fun loadHistory(): MutableMap<String, MutableList<Long>> {
        val json = prefs.getString("data", null) ?: return mutableMapOf()
        val result = mutableMapOf<String, MutableList<Long>>()
        val obj = JSONObject(json)
        for (key in obj.keys()) {
            val arr = obj.getJSONArray(key)
            val list = mutableListOf<Long>()
            for (i in 0 until arr.length()) list.add(arr.getLong(i))
            result[key] = list
        }
        return result
    }

    private fun saveHistory(history: Map<String, List<Long>>) {
        val cutoff = System.currentTimeMillis() - tenDaysMs
        val obj = JSONObject()
        for ((pkg, timestamps) in history) {
            val recent = timestamps.filter { it >= cutoff }
            if (recent.isNotEmpty()) {
                val arr = JSONArray()
                recent.forEach { arr.put(it) }
                obj.put(pkg, arr)
            }
        }
        prefs.edit().putString("data", obj.toString()).apply()
    }
}
