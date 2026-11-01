package com.koussa.islamicazkar.update

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.pm.PackageInfoCompat
import com.koussa.islamicazkar.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

/** GitHub build: updates itself from the latest release of the public releases repository. */
fun appUpdater(): Updater = GitHubUpdater

private object GitHubUpdater : Updater {
    private const val LATEST = "https://api.github.com/repos/Mkuz1/islamic-azkar-releases/releases/latest"
    private const val ASSET = "IslamicAzkar.apk"

    override val supported = true

    override suspend fun newerRelease(context: Context): Release? = withContext(Dispatchers.IO) {
        val conn = (URL(LATEST).openConnection() as HttpURLConnection).apply {
            connectTimeout = 15_000; readTimeout = 15_000
            setRequestProperty("Accept", "application/vnd.github+json")
            setRequestProperty("User-Agent", "IslamicAzkar/${BuildConfig.VERSION_NAME}")
        }
        try {
            if (conn.responseCode != 200) throw IOException("GitHub answered ${conn.responseCode}")
            val o = JSONObject(conn.inputStream.bufferedReader().use { it.readText() })
            if (o.optBoolean("draft") || o.optBoolean("prerelease")) return@withContext null
            val tag = o.getString("tag_name")
            val assets = o.getJSONArray("assets")
            val apk = (0 until assets.length()).map { assets.getJSONObject(it) }.firstOrNull { it.getString("name") == ASSET }
                ?: return@withContext null
            if (!isNewer(tag, BuildConfig.VERSION_NAME)) return@withContext null
            Release(tag, tag.removePrefix("v"), o.optString("body"), apk.getString("browser_download_url"), o.optString("html_url"))
        } finally {
            conn.disconnect()
        }
    }

    override suspend fun downloadAndInstall(context: Context, release: Release, onProgress: (Float) -> Unit) {
        val app = context.applicationContext
        // Android 8+: the user must allow this app to install apps ("Install unknown apps") once.
        if (!app.packageManager.canRequestPackageInstalls()) {
            app.startActivity(
                Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:${app.packageName}"))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            )
            throw NeedsPermission()
        }
        val apk = download(app, release, onProgress)
        checkApk(app, apk)
        install(app, apk)
    }

    private suspend fun download(context: Context, release: Release, onProgress: (Float) -> Unit): File = withContext(Dispatchers.IO) {
        val dir = File(context.cacheDir, "updates").apply { mkdirs(); listFiles()?.forEach { it.delete() } }
        val file = File(dir, ASSET)
        var url = URL(release.apkUrl)
        var conn: HttpURLConnection
        // GitHub redirects asset downloads to its CDN (possibly more than once).
        var hops = 0
        while (true) {
            conn = (url.openConnection() as HttpURLConnection).apply {
                instanceFollowRedirects = false; connectTimeout = 20_000; readTimeout = 30_000
                setRequestProperty("User-Agent", "IslamicAzkar/${BuildConfig.VERSION_NAME}")
            }
            val code = conn.responseCode
            if (code in 300..399 && hops++ < 5) { url = URL(url, conn.getHeaderField("Location")); conn.disconnect(); continue }
            if (code != 200) throw IOException("Download answered $code")
            break
        }
        val total = conn.contentLengthLong
        conn.inputStream.use { input ->
            file.outputStream().use { out ->
                val buf = ByteArray(64 * 1024)
                var done = 0L
                while (true) {
                    val n = input.read(buf)
                    if (n < 0) break
                    out.write(buf, 0, n)
                    done += n
                    if (total > 0) onProgress(done.toFloat() / total)
                }
            }
        }
        conn.disconnect()
        file
    }

    /** Refuses files that are not a newer build of this same app (the installer also checks the signature). */
    private fun checkApk(context: Context, apk: File) {
        val pm = context.packageManager
        val info = pm.getPackageArchiveInfo(apk.path, 0) ?: throw IOException("Not a valid APK")
        if (info.packageName != context.packageName) throw IOException("APK is for ${info.packageName}")
        val installed = PackageInfoCompat.getLongVersionCode(pm.getPackageInfo(context.packageName, 0))
        if (PackageInfoCompat.getLongVersionCode(info) <= installed) throw IOException("APK is not newer than the installed version")
    }

    private fun install(context: Context, apk: File) {
        val installer = context.packageManager.packageInstaller
        val params = PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL).apply {
            setAppPackageName(context.packageName)
            setSize(apk.length())
            if (Build.VERSION.SDK_INT >= 31) setRequireUserAction(PackageInstaller.SessionParams.USER_ACTION_NOT_REQUIRED)
        }
        val id = installer.createSession(params)
        installer.openSession(id).use { session ->
            session.openWrite(ASSET, 0, apk.length()).use { out ->
                apk.inputStream().use { it.copyTo(out) }
                session.fsync(out)
            }
            val flags = PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= 31) PendingIntent.FLAG_MUTABLE else 0)
            val status = PendingIntent.getBroadcast(context, id, Intent(context, InstallStatusReceiver::class.java), flags)
            session.commit(status.intentSender)
        }
    }
}

/** Thrown when the user first has to allow installing apps from this app. */
class NeedsPermission : IOException("install permission needed")

/** Receives the installer's result: shows the system confirmation, or reports a failure. */
class InstallStatusReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.getIntExtra(PackageInstaller.EXTRA_STATUS, PackageInstaller.STATUS_FAILURE)) {
            PackageInstaller.STATUS_PENDING_USER_ACTION -> {
                @Suppress("DEPRECATION")
                val confirm = if (Build.VERSION.SDK_INT >= 33) intent.getParcelableExtra(Intent.EXTRA_INTENT, Intent::class.java)
                else intent.getParcelableExtra(Intent.EXTRA_INTENT)
                confirm?.let { context.startActivity(it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
            }
            PackageInstaller.STATUS_SUCCESS -> UpdateBus.state.value = UpdateState.Idle // the app restarts as the new version
            else -> UpdateBus.state.value = UpdateState.Failed(intent.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE))
        }
    }
}
