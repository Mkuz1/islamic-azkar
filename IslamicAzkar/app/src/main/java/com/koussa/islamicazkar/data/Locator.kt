package com.koussa.islamicazkar.data

import android.annotation.SuppressLint
import android.content.Context
import android.location.Geocoder
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.Looper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.util.Locale
import java.util.TimeZone
import kotlin.coroutines.resume

/** Finds the phone's approximate position (coarse location is enough for prayer times). */
object Locator {
    @SuppressLint("MissingPermission")
    suspend fun find(ctx: Context): Place? {
        val lm = ctx.getSystemService(LocationManager::class.java) ?: return null
        val providers = listOf(LocationManager.NETWORK_PROVIDER, LocationManager.GPS_PROVIDER, LocationManager.PASSIVE_PROVIDER)
            .filter { runCatching { lm.isProviderEnabled(it) }.getOrDefault(false) }
        val last = providers.mapNotNull { runCatching { lm.getLastKnownLocation(it) }.getOrNull() }.maxByOrNull { it.time }
        val fresh = last?.takeIf { System.currentTimeMillis() - it.time < 6 * 3_600_000L }
        val loc = fresh ?: providers.firstOrNull { it != LocationManager.PASSIVE_PROVIDER }?.let { current(ctx, lm, it) } ?: last ?: return null
        val (ar, en) = withContext(Dispatchers.IO) { name(ctx, loc, Locale("ar")) to name(ctx, loc, Locale.ENGLISH) }
        return Place(ar ?: en ?: "", en ?: ar ?: "", loc.latitude, loc.longitude, TimeZone.getDefault().id)
    }

    @SuppressLint("MissingPermission")
    private suspend fun current(ctx: Context, lm: LocationManager, provider: String): Location? = withTimeoutOrNull(20_000) {
        suspendCancellableCoroutine { cont ->
            if (Build.VERSION.SDK_INT >= 30) {
                lm.getCurrentLocation(provider, null, ctx.mainExecutor) { if (cont.isActive) cont.resume(it) }
            } else {
                val listener = object : LocationListener {
                    override fun onLocationChanged(l: Location) { if (cont.isActive) cont.resume(l) }
                    @Deprecated("Deprecated in Java") override fun onStatusChanged(p: String?, s: Int, e: android.os.Bundle?) {}
                    override fun onProviderEnabled(p: String) {}
                    override fun onProviderDisabled(p: String) { if (cont.isActive) cont.resume(null) }
                }
                @Suppress("DEPRECATION")
                lm.requestSingleUpdate(provider, listener, Looper.getMainLooper())
                cont.invokeOnCancellation { lm.removeUpdates(listener) }
            }
        }
    }

    /** The town's name, when the phone can look it up. */
    @Suppress("DEPRECATION")
    private fun name(ctx: Context, loc: Location, locale: Locale): String? = runCatching {
        if (!Geocoder.isPresent()) return null
        val a = Geocoder(ctx, locale).getFromLocation(loc.latitude, loc.longitude, 1)?.firstOrNull() ?: return null
        a.locality ?: a.subAdminArea ?: a.adminArea
    }.getOrNull()
}
