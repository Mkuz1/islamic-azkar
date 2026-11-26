package com.koussa.islamicazkar.update

import android.content.Context

/** Play Store build: updates come from Google Play, so there is no self-update code here. */
fun appUpdater(): Updater = object : Updater {
    override val supported = false
    override suspend fun newerRelease(context: Context): Release? = null
    override suspend fun downloadAndInstall(context: Context, release: Release, onProgress: (Float) -> Unit) = Unit
}
