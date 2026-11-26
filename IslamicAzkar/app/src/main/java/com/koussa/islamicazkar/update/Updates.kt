package com.koussa.islamicazkar.update

import android.content.Context
import com.koussa.islamicazkar.data.Lang
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** A newer build published on GitHub. */
data class Release(val tag: String, val version: String, val notes: String, val apkUrl: String, val pageUrl: String)

/** Progress of a download-and-install, shared between the UI and the install-status receiver. */
sealed interface UpdateState {
    data object Idle : UpdateState
    data object Checking : UpdateState
    data object UpToDate : UpdateState
    data class Available(val release: Release) : UpdateState
    data class Downloading(val release: Release, val progress: Float) : UpdateState
    data class Installing(val release: Release) : UpdateState
    data class Failed(val message: String?) : UpdateState
}

/**
 * Self-updating from the GitHub releases. Only the "github" build flavor implements it:
 * Google Play forbids apps updating themselves outside Play, so the Play build gets an
 * updater that does nothing and keeps updating through the Play Store.
 */
interface Updater {
    /** False in the Play build: no update UI is shown at all. */
    val supported: Boolean

    /** The latest release if it is newer than the installed version, else null. Throws on network errors. */
    suspend fun newerRelease(context: Context): Release?

    /** Downloads the APK and hands it to the system installer, which asks the user to confirm. */
    suspend fun downloadAndInstall(context: Context, release: Release, onProgress: (Float) -> Unit)
}

object UpdateBus {
    val state = MutableStateFlow<UpdateState>(UpdateState.Idle)
    val current: StateFlow<UpdateState> get() = state
}

/** Compares dotted versions numerically ("1.0.10" > "1.0.9"); a missing part counts as 0. */
fun isNewer(candidate: String, installed: String): Boolean {
    fun parts(v: String) = v.trim().removePrefix("v").removePrefix("V").split('.', '-', ' ').map { p -> p.takeWhile(Char::isDigit).toIntOrNull() ?: 0 }
    val a = parts(candidate)
    val b = parts(installed)
    for (i in 0 until maxOf(a.size, b.size)) {
        val x = a.getOrElse(i) { 0 }
        val y = b.getOrElse(i) { 0 }
        if (x != y) return x > y
    }
    return false
}

class UpdateTexts(
    val available: String,
    val version: (String) -> String,
    val install: String,
    val later: String,
    val downloading: String,
    val installing: String,
    val failed: String,
    val check: String,
    val checking: String,
    val upToDate: String,
    val current: (String) -> String,
    val allowInstall: String,
)

fun updateTexts(lang: Lang): UpdateTexts = when (lang) {
    Lang.AR -> UpdateTexts(
        available = "يتوفّر إصدار جديد", version = { "الإصدار $it" }, install = "تنزيل وتثبيت", later = "لاحقًا",
        downloading = "جارٍ التنزيل…", installing = "جارٍ التثبيت…", failed = "تعذّر التحديث، حاول مرة أخرى.",
        check = "البحث عن تحديثات", checking = "جارٍ البحث…", upToDate = "لديك أحدث إصدار", current = { "الإصدار الحالي $it" },
        allowInstall = "اسمح للتطبيق بتثبيت التحديثات ثم اضغط «تنزيل وتثبيت» مرة أخرى.",
    )
    Lang.EN -> UpdateTexts(
        available = "A new version is available", version = { "Version $it" }, install = "Download and install", later = "Later",
        downloading = "Downloading…", installing = "Installing…", failed = "The update failed. Please try again.",
        check = "Check for updates", checking = "Checking…", upToDate = "You have the latest version", current = { "Installed version $it" },
        allowInstall = "Allow the app to install updates, then tap “Download and install” again.",
    )
    Lang.FR -> UpdateTexts(
        available = "Une nouvelle version est disponible", version = { "Version $it" }, install = "Télécharger et installer", later = "Plus tard",
        downloading = "Téléchargement…", installing = "Installation…", failed = "La mise à jour a échoué. Réessayez.",
        check = "Rechercher des mises à jour", checking = "Recherche…", upToDate = "Vous avez la dernière version", current = { "Version installée $it" },
        allowInstall = "Autorisez l'application à installer des mises à jour, puis touchez « Télécharger et installer » à nouveau.",
    )
}
