package com.koussa.islamicazkar.ui

import android.Manifest
import android.appwidget.AppWidgetManager
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import android.content.ComponentName
import android.content.Context
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.koussa.islamicazkar.R
import com.koussa.islamicazkar.data.Lang
import com.koussa.islamicazkar.i18n.Strings
import com.koussa.islamicazkar.i18n.More
import com.koussa.islamicazkar.i18n.more
import com.koussa.islamicazkar.ui.glass.Glass
import com.koussa.islamicazkar.ui.glass.pressable
import com.koussa.islamicazkar.widget.AzkarCardsWidget
import com.koussa.islamicazkar.widget.CounterStatsWidget
import com.koussa.islamicazkar.widget.DayAzkarWidget
import com.koussa.islamicazkar.widget.DhikrWidget
import com.koussa.islamicazkar.widget.DigitalClockWidget
import com.koussa.islamicazkar.widget.HijriDateWidget
import com.koussa.islamicazkar.widget.IslamicClockWidget
import com.koussa.islamicazkar.widget.MorningAzkarWidget
import com.koussa.islamicazkar.widget.NightAzkarWidget
import com.koussa.islamicazkar.widget.PrayerTimesWidget
import com.koussa.islamicazkar.widget.ProgressWidget
import com.koussa.islamicazkar.widget.QuickWidget
import com.koussa.islamicazkar.widget.ScriptureWidget
import com.koussa.islamicazkar.widget.TasbihWidget
import com.koussa.islamicazkar.widget.TimelineAzkarWidget

/** One widget in the gallery: its provider, picker preview, name and a line about what it does. */
private class WidgetEntry(
    val provider: Class<*>,
    val preview: Int,
    val title: (Strings, More) -> String,
    val ar: String,
    val en: String,
    val fr: String,
) {
    fun about(lang: Lang) = when (lang) { Lang.AR -> ar; Lang.EN -> en; Lang.FR -> fr }
}

private class WidgetGroup(val ar: String, val en: String, val fr: String, val entries: List<WidgetEntry>) {
    fun title(lang: Lang) = when (lang) { Lang.AR -> ar; Lang.EN -> en; Lang.FR -> fr }
}

private val GALLERY = listOf(
    WidgetGroup(
        "الصلاة والوقت", "Prayer & time", "Prière & temps",
        listOf(
            WidgetEntry(PrayerTimesWidget::class.java, R.drawable.widget_preview_prayer, { _, m -> m.widgetPrayer },
                "الصلاة القادمة مع عدٍّ تنازلي. غيّر حجمها من حلقة صغيرة إلى جدول كامل.",
                "The next prayer with a live countdown. Resize it from a small ring to a full timetable.",
                "La prochaine prière avec compte à rebours. Redimensionnez de l’anneau au tableau complet."),
            WidgetEntry(HijriDateWidget::class.java, R.drawable.widget_preview_hijri, { s, _ -> s.widgetHijri },
                "التاريخ الهجري واليوم بخطٍّ عربيٍّ جميل.",
                "The Hijri date with the weekday in calligraphy.",
                "La date hégirienne et le jour en calligraphie."),
            WidgetEntry(IslamicClockWidget::class.java, R.drawable.widget_preview_clock, { s, _ -> s.widgetClock },
                "ساعة على نجمة ثمانية بأرقام عربية.",
                "An analogue clock on an eight-pointed star, with Arabic numerals.",
                "Une horloge sur une étoile à huit branches, en chiffres arabes."),
            WidgetEntry(DigitalClockWidget::class.java, R.drawable.widget_preview_digital, { _, m -> m.widgetDigital },
                "الوقت بخطٍّ كبير مع التاريخين الهجري والميلادي.",
                "The time in large type with the Hijri and Gregorian dates.",
                "L’heure en grand avec les dates hégirienne et grégorienne."),
            WidgetEntry(ProgressWidget::class.java, R.drawable.widget_preview_progress, { _, m -> m.widgetProgress },
                "حلقاتٌ تُظهر ما مضى من اليوم والشهر والسنة.",
                "Rings showing how much of the day, month and year has passed.",
                "Des anneaux montrant l’avancée du jour, du mois et de l’année."),
        ),
    ),
    WidgetGroup(
        "الأذكار", "Azkar", "Adhkar",
        listOf(
            WidgetEntry(TimelineAzkarWidget::class.java, R.drawable.widget_preview_timeline, { s, _ -> s.widgetTimeline },
                "تتغيّر مع أوقات اليوم: صباح، نهار، مساء، ليل.",
                "Changes with the time of day: morning, day, evening and night.",
                "Suit le moment de la journée : matin, jour, soir et nuit."),
            WidgetEntry(MorningAzkarWidget::class.java, R.drawable.widget_preview_morning, { s, _ -> s.morningAzkar },
                "أذكار الصباح واحداً تلو الآخر مع التقدّم.",
                "Morning azkar one by one, with your progress.",
                "Les adhkar du matin un par un, avec votre progression."),
            WidgetEntry(DayAzkarWidget::class.java, R.drawable.widget_preview_day, { s, _ -> s.dayAzkar },
                "أذكارٌ تُقال خلال اليوم.",
                "Azkar to say through the day.",
                "Des adhkar à dire au fil de la journée."),
            WidgetEntry(NightAzkarWidget::class.java, R.drawable.widget_preview_night, { s, _ -> s.nightAzkar },
                "أذكار المساء وقبل النوم.",
                "Evening and before-sleep azkar.",
                "Les adhkar du soir et avant de dormir."),
            WidgetEntry(AzkarCardsWidget::class.java, R.drawable.widget_preview_cards, { s, _ -> s.widgetCards },
                "أذكار مع شريط أقسام: صباح، مساء، نوم، صلاة، نهار.",
                "Azkar with a category bar: morning, evening, sleep, prayer and day.",
                "Des adhkar avec une barre de catégories : matin, soir, sommeil, prière, jour."),
            WidgetEntry(DhikrWidget::class.java, R.drawable.widget_preview_dhikr, { s, _ -> s.widgetDhikr },
                "ذكرٌ جديد كلّ يوم.",
                "A new dhikr every day.",
                "Un nouveau dhikr chaque jour."),
            WidgetEntry(QuickWidget::class.java, R.drawable.widget_preview_quick, { s, _ -> s.widgetQuick },
                "وصولٌ سريع لأذكار الصباح والمساء والنوم.",
                "One tap to the morning, evening or sleep azkar.",
                "Un accès direct aux adhkar du matin, du soir et du sommeil."),
        ),
    ),
    WidgetGroup(
        "القرآن والحكمة", "Quran & wisdom", "Coran & sagesse",
        listOf(
            WidgetEntry(ScriptureWidget::class.java, R.drawable.widget_preview_scripture, { _, m -> m.widgetQuotes },
                "قرآنٌ وحديثٌ وذكرٌ وشعرٌ وحكمة: اضغط لتتجدّد الكلمات.",
                "Quran, hadith, azkar, poetry and wisdom: tap for a new one.",
                "Coran, hadith, adhkar, poésie et sagesse : touchez pour en changer."),
        ),
    ),
    WidgetGroup(
        "التسبيح", "Tasbih", "Tasbih",
        listOf(
            WidgetEntry(TasbihWidget::class.java, R.drawable.widget_preview_tasbih, { s, _ -> s.widgetTasbih },
                "سبّح من شاشتك الرئيسية مباشرة.",
                "Count your tasbih right from the home screen.",
                "Comptez votre tasbih depuis l’écran d’accueil."),
            WidgetEntry(CounterStatsWidget::class.java, R.drawable.widget_preview_counter, { s, _ -> s.widgetCounter },
                "عدّادٌ مع إحصاءٍ أسبوعي.",
                "A counter with a weekly chart.",
                "Un compteur avec le graphique de la semaine."),
        ),
    ),
)

private fun unsupported(lang: Lang) = when (lang) {
    Lang.AR -> "لا يدعم مشغّل الشاشة الإضافة المباشرة. اضغط مطولاً على الشاشة الرئيسية واختر «الأدوات»."
    Lang.EN -> "Your launcher can’t add widgets directly. Long-press the home screen and choose “Widgets”."
    Lang.FR -> "Votre lanceur ne permet pas l’ajout direct. Appuyez longuement sur l’écran d’accueil et choisissez « Widgets »."
}

private fun tabTitle(lang: Lang) = when (lang) { Lang.AR -> "الأدوات"; Lang.EN -> "Widgets"; Lang.FR -> "Widgets" }

/** Asks the launcher to place [provider] on the home screen; false when the launcher can't. */
fun pinWidget(ctx: Context, provider: Class<*>): Boolean {
    val mgr = AppWidgetManager.getInstance(ctx)
    if (!mgr.isRequestPinAppWidgetSupported) return false
    return mgr.requestPinAppWidget(ComponentName(ctx, provider), null, null)
}

/** The widget gallery: every widget with its preview and a button that adds it to the home screen. */
@Composable
fun WidgetsScreen() {
    val app = LocalApp.current
    val ctx = LocalContext.current
    val s = Glass.s
    val lang = Glass.lang
    val m = more(lang)
    val title = tabTitle(lang)
    val listState = rememberLazyListState()
    // Runs [action] once notifications are allowed (asking on Android 13+).
    var pending by remember { mutableStateOf<(() -> Unit)?>(null) }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) pending?.invoke() else app.toast = s.permissionDenied
        pending = null
    }
    fun withNotifications(action: () -> Unit) {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            pending = action
            permission.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else action()
    }
    CollapsingTitle(title, listState)
    LazyColumn(Modifier.fillMaxSize(), state = listState, contentPadding = screenPadding()) {
        item { LargeTitle(title, s.widgetsHint) }
        GALLERY.forEach { group ->
            item { SectionHeader(group.title(lang)) }
            items(group.entries, key = { it.provider.name }) { e ->
                WidgetCard(e.preview, e.title(s, m), e.about(lang), s.addToHome) {
                    if (!pinWidget(ctx, e.provider)) app.toast = unsupported(lang)
                }
            }
        }
        item { Spacer(Modifier.padding(top = 8.dp)) }
        item { WidgetStyleSettings(::withNotifications) }
    }
}

@Composable
private fun WidgetCard(preview: Int, title: String, about: String, add: String, onAdd: () -> Unit) {
    val g = Glass.colors
    Card(Modifier.padding(bottom = 14.dp)) {
        Column {
            // The picker preview on a photo-like wallpaper, so the glass reads as it will at home.
            Box(
                Modifier
                    .fillMaxWidth()
                    .padding(10.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(Color(0xFF5B7FA6), Color(0xFFB98A6E), Color(0xFF3E6B5A)),
                            start = Offset.Zero, end = Offset(900f, 700f),
                        ),
                    )
                    .padding(horizontal = 14.dp, vertical = 16.dp),
                contentAlignment = Alignment.Center,
            ) {
                Image(
                    painterResource(preview), contentDescription = title,
                    contentScale = ContentScale.Fit, modifier = Modifier.fillMaxWidth().heightIn(max = 210.dp),
                )
            }
            Row(Modifier.padding(start = 16.dp, end = 12.dp, bottom = 14.dp, top = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(title, color = g.text, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                    Text(about, color = g.text2, fontSize = 13.sp, lineHeight = 18.sp, modifier = Modifier.padding(top = 2.dp))
                }
                Spacer(Modifier.width(10.dp))
                Row(
                    Modifier
                        .clip(RoundedCornerShape(50))
                        .background(g.accent)
                        .pressable(onClick = onAdd, squish = true)
                        .padding(start = 12.dp, end = 16.dp, top = 9.dp, bottom = 9.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Icon(Icons.Rounded.Add, null, tint = Color.White, modifier = Modifier.padding(0.dp))
                    Text(add, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}
