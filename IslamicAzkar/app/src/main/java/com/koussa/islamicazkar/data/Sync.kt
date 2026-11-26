package com.koussa.islamicazkar.data

import android.content.Context
import androidx.compose.runtime.mutableStateOf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * Syncs the azkar record ([AzkarHistory]) between a person's devices — Android, iPhone, iPad, Mac and
 * the website — through islamicazkar.net. No account: the devices share a private 12-character sync
 * code; the server keeps only the record (days → completed readings) under a hash of it. Once a
 * device has a code, syncing is automatic: when a reading is completed, when the app comes back to
 * the front and when the Record tab opens.
 */
object AzkarSync {
    private const val BASE = "https://islamicazkar.net/"
    private const val K_CODE = "sync_code"
    private const val K_LAST = "sync_last"
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /** Observed by the Settings screen. */
    val state = mutableStateOf<String?>(null)
    val lastSync = mutableStateOf(0L)

    class UnknownCode : Exception()

    fun code(ctx: Context): String? = Settings.prefs(ctx).getString(K_CODE, null).also { state.value = it }
    fun last(ctx: Context): Long = Settings.prefs(ctx).getLong(K_LAST, 0L).also { lastSync.value = it }

    fun display(code: String) = code.chunked(4).joinToString("-")
    fun link(code: String) = "islamicazkar://sync/$code"
    fun normalize(s: String) = s.uppercase().filter { it in 'A'..'Z' || it in '0'..'9' }

    /** Turns sync on with a new code, uploading this device's record. */
    suspend fun start(ctx: Context): String = withContext(Dispatchers.IO) {
        val r = post("sync/new", JSONObject().put("days", AzkarHistory.readingsJson(ctx)))
        val code = r.getString("code")
        Settings.prefs(ctx).edit().putString(K_CODE, code).apply()
        apply(ctx, r)
        code
    }

    /** Links this device to another device's code, merging both records. */
    suspend fun link(ctx: Context, typed: String) = withContext(Dispatchers.IO) {
        val c = normalize(typed)
        if (c.length != 12) throw UnknownCode()
        val r = post("sync", JSONObject().put("code", c).put("days", AzkarHistory.readingsJson(ctx)))
        Settings.prefs(ctx).edit().putString(K_CODE, c).apply()
        apply(ctx, r)
    }

    /** Sends this device's record and takes in the other devices'. Quietly does nothing offline. */
    suspend fun syncNow(ctx: Context) = withContext(Dispatchers.IO) {
        val c = code(ctx) ?: return@withContext
        try {
            apply(ctx, post("sync", JSONObject().put("code", c).put("days", AzkarHistory.readingsJson(ctx))))
        } catch (_: UnknownCode) {
            stop(ctx)   // the code no longer exists on the server
        } catch (_: Exception) {
        }
    }

    fun syncInBackground(ctx: Context) {
        val app = ctx.applicationContext
        scope.launch { syncNow(app) }
    }

    /** Turns sync off on this device (the record stays here and on the other devices). */
    fun stop(ctx: Context) {
        Settings.prefs(ctx).edit().remove(K_CODE).remove(K_LAST).apply()
        state.value = null
        lastSync.value = 0L
    }

    private fun apply(ctx: Context, r: JSONObject) {
        val days = r.optJSONObject("days") ?: return
        AzkarHistory.merge(ctx, days)
        val now = System.currentTimeMillis()
        Settings.prefs(ctx).edit().putLong(K_LAST, now).apply()
        state.value = Settings.prefs(ctx).getString(K_CODE, null)
        lastSync.value = now
    }

    private fun post(path: String, body: JSONObject): JSONObject {
        val conn = (URL(BASE + path).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 15_000
            readTimeout = 15_000
            doOutput = true
            setRequestProperty("content-type", "application/json")
        }
        try {
            conn.outputStream.use { it.write(body.toString().toByteArray()) }
            val status = conn.responseCode
            if (status == 404) throw UnknownCode()
            if (status != 200) throw java.io.IOException("HTTP $status")
            return JSONObject(conn.inputStream.bufferedReader().use { it.readText() })
        } finally {
            conn.disconnect()
        }
    }
}

/** Day → readings for the server. */
internal fun AzkarHistory.readingsJson(ctx: Context): JSONObject =
    JSONObject().also { o -> all(ctx).forEach { (day, list) -> if (list.isNotEmpty()) o.put(day, JSONArray(list)) } }
