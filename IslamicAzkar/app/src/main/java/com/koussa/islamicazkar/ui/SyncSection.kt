package com.koussa.islamicazkar.ui

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.LinkOff
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.koussa.islamicazkar.data.AzkarSync
import com.koussa.islamicazkar.data.Lang
import com.koussa.islamicazkar.ui.glass.Glass
import com.koussa.islamicazkar.ui.glass.pressable
import com.koussa.islamicazkar.ui.glass.dpadLeavesField
import kotlinx.coroutines.launch
import java.text.DateFormat
import java.util.Date

/** Texts for syncing between devices (the same wording as the iPhone app). */
class SyncTexts(
    val title: String, val intro: String, val turnOn: String, val haveCode: String, val enterCode: String, val link: String,
    val linking: String, val yourCode: String, val scanHint: String, val syncNow: String, val lastSync: String, val never: String,
    val stop: String, val stopConfirm: String, val badCode: String, val offline: String, val linked: String,
) {
    companion object {
        fun of(lang: Lang) = when (lang) {
            Lang.AR -> SyncTexts(
                "المزامنة بين الأجهزة",
                "اعرض سجلّ أذكارك على كل أجهزتك: آيفون وآيباد وماك وأندرويد والموقع. بلا حساب ولا بريد: رمز خاص يربط أجهزتك، ثم تتم المزامنة تلقائيًا.",
                "تفعيل المزامنة", "لديّ رمز من جهاز آخر", "أدخل رمز المزامنة", "ربط", "جارٍ الربط…", "رمز المزامنة",
                "على جهازك الآخر: امسح الرمز بالكاميرا، أو أدخله في الإعدادات ← المزامنة (وفي الموقع: سجلّي ← المزامنة). احتفظ به لنفسك.",
                "زامن الآن", "آخر مزامنة", "لم تتم بعد", "إيقاف المزامنة على هذا الجهاز", "يبقى سجلّك على هذا الجهاز وعلى أجهزتك الأخرى.",
                "الرمز غير صحيح. تأكد منه وحاول مرة أخرى.", "تعذّر الاتصال. تحقق من الإنترنت وحاول مرة أخرى.", "تم ربط هذا الجهاز")
            Lang.EN -> SyncTexts(
                "Sync between devices",
                "See your azkar record on all your devices: iPhone, iPad, Mac, Android and the website. No account or email: a private code links your devices, then they sync automatically.",
                "Turn on sync", "I have a code from another device", "Enter the sync code", "Link", "Linking…", "Sync code",
                "On your other device, scan this with the camera, or enter the code in Settings → Sync (on the website: Record → Sync). Keep it to yourself.",
                "Sync now", "Last synced", "not yet", "Stop syncing on this device", "Your record stays on this device and on your other devices.",
                "That code isn't right. Check it and try again.", "Couldn't connect. Check your internet and try again.", "This device is linked")
            Lang.FR -> SyncTexts(
                "Synchronisation entre appareils",
                "Retrouvez votre historique d'adhkar sur tous vos appareils : iPhone, iPad, Mac, Android et le site. Sans compte ni e-mail : un code privé relie vos appareils, puis tout se synchronise automatiquement.",
                "Activer la synchronisation", "J'ai un code d'un autre appareil", "Saisissez le code de synchronisation", "Relier", "Liaison…", "Code de synchronisation",
                "Sur votre autre appareil, scannez-le avec l'appareil photo, ou saisissez le code dans Réglages → Synchronisation (sur le site : Historique → Synchronisation). Gardez-le pour vous.",
                "Synchroniser", "Dernière synchronisation", "pas encore", "Arrêter la synchronisation sur cet appareil", "Votre historique reste sur cet appareil et sur vos autres appareils.",
                "Ce code n'est pas valide. Vérifiez-le et réessayez.", "Connexion impossible. Vérifiez Internet et réessayez.", "Cet appareil est relié")
        }
    }
}

/** A QR code (black on white) for [text]. */
private fun qrBitmap(text: String, size: Int = 480): Bitmap {
    val m = QRCodeWriter().encode(text, BarcodeFormat.QR_CODE, size, size, mapOf(EncodeHintType.MARGIN to 1))
    val px = IntArray(size * size) { i -> if (m[i % size, i / size]) 0xFF000000.toInt() else 0xFFFFFFFF.toInt() }
    return Bitmap.createBitmap(px, size, size, Bitmap.Config.ARGB_8888)
}

/** Settings section: turn sync on (shows the code and its QR), or link with another device's code. */
@Composable
fun SyncSection() {
    val ctx = LocalContext.current
    val app = LocalApp.current
    val g = Glass.colors
    val t = SyncTexts.of(Glass.lang)
    val scope = rememberCoroutineScope()
    val code by AzkarSync.state
    val last by AzkarSync.lastSync
    var busy by remember { mutableStateOf(false) }
    var typing by remember { mutableStateOf(false) }
    var typed by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var confirmStop by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { AzkarSync.code(ctx); AzkarSync.last(ctx) }

    fun link() {
        scope.launch {
            busy = true; error = null
            try { AzkarSync.link(ctx, typed); typed = ""; typing = false; app.toast = t.linked }
            catch (e: AzkarSync.UnknownCode) { error = t.badCode }
            catch (e: Exception) { error = t.offline }
            busy = false
        }
    }

    SectionHeader(t.title)
    InsetGroup {
        val c = code
        if (c != null) {
            Column(Modifier.fillMaxWidth().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(t.yourCode, color = g.text2, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                Text(AzkarSync.display(c), color = g.text, fontSize = 24.sp, fontWeight = FontWeight.SemiBold, fontFamily = FontFamily.Monospace,
                    modifier = Modifier.padding(vertical = 6.dp))
                val qr = remember(c) { qrBitmap(AzkarSync.link(c)).asImageBitmap() }
                Image(qr, null, filterQuality = FilterQuality.None,
                    modifier = Modifier.size(170.dp).clip(RoundedCornerShape(14.dp)).background(Color.White).padding(8.dp))
                Spacer(Modifier.height(10.dp))
                Text(t.scanHint, color = g.text2, fontSize = 13.sp, lineHeight = 18.sp, textAlign = TextAlign.Center)
            }
            RowDivider(inset = 16.dp)
            ListRow(
                t.syncNow, icon = Icons.Rounded.Sync, iconColor = Color(0xFF34C759),
                value = if (last > 0) DateFormat.getTimeInstance(DateFormat.SHORT).format(Date(last)) else t.never,
                onClick = { if (!busy) scope.launch { busy = true; AzkarSync.syncNow(ctx); busy = false } },
            )
            RowDivider()
            if (!confirmStop) {
                ListRow(t.stop, icon = Icons.Rounded.LinkOff, iconColor = Color(0xFFFF3B30), onClick = { confirmStop = true })
            } else {
                Column(Modifier.padding(16.dp)) {
                    Text(t.stopConfirm, color = g.text2, fontSize = 14.sp)
                    Spacer(Modifier.height(10.dp))
                    Text(t.stop, color = Color(0xFFFF3B30), fontSize = 16.sp, fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.pressable({ AzkarSync.stop(ctx); confirmStop = false }).padding(vertical = 6.dp))
                }
            }
        } else {
            Text(t.intro, color = g.text2, fontSize = 14.sp, lineHeight = 19.sp, modifier = Modifier.padding(16.dp))
            RowDivider(inset = 16.dp)
            ListRow(
                if (busy && !typing) "…" else t.turnOn, icon = Icons.Rounded.Sync, iconColor = Color(0xFF34C759),
                onClick = {
                    if (!busy) scope.launch {
                        busy = true; error = null
                        try { AzkarSync.start(ctx) } catch (e: Exception) { error = t.offline }
                        busy = false
                    }
                },
            )
            RowDivider()
            if (!typing) {
                ListRow(t.haveCode, icon = Icons.Rounded.Link, iconColor = Color(0xFF007AFF), onClick = { typing = true })
            } else {
                Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    BasicTextField(
                        typed, { typed = it.take(16) },
                        singleLine = true,
                        textStyle = TextStyle(color = g.text, fontSize = 18.sp, fontFamily = FontFamily.Monospace),
                        cursorBrush = SolidColor(g.accent),
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters, autoCorrect = false, imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { link() }),
                        decorationBox = { inner ->
                            if (typed.isEmpty()) Text(t.enterCode, color = g.text3, fontSize = 16.sp)
                            inner()
                        },
                        modifier = Modifier.weight(1f).dpadLeavesField().clip(RoundedCornerShape(10.dp)).background(g.fillGray).padding(horizontal = 12.dp, vertical = 10.dp),
                    )
                    Spacer(Modifier.width(10.dp))
                    val ready = !busy && AzkarSync.normalize(typed).length == 12
                    Text(if (busy) t.linking else t.link, color = if (ready) g.accent else g.text3, fontSize = 17.sp, fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.pressable({ if (ready) link() }, enabled = ready).padding(8.dp))
                }
            }
        }
        error?.let { Text(it, color = Color(0xFFFF3B30), fontSize = 13.sp, modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 12.dp)) }
    }
}
