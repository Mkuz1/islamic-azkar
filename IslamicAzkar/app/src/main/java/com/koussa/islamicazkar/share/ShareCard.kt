package com.koussa.islamicazkar.share

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.text.Layout
import android.text.StaticLayout
import android.text.TextDirectionHeuristics
import android.text.TextPaint
import androidx.core.content.FileProvider
import androidx.core.content.res.ResourcesCompat
import com.koussa.islamicazkar.R
import com.koussa.islamicazkar.data.Lang
import com.koussa.islamicazkar.data.Zikr
import java.io.File
import kotlin.math.cos
import kotlin.math.sin

/** Renders a dhikr as a 1080×1350 share image: a glass card over a luminous backdrop, signed with the app name. */
object ShareCard {

    class Style(
        val bg: Int,
        val cardTop: Int,
        val cardBottom: Int,
        val pattern: Int,
        val frame: Int,
        val text: Int,
        val text2: Int,
        val accent: Int,
        val onAccentBg: Int,
    )

    val styles = listOf(
        // Emerald — the Mushaf's opening page
        Style(0xFF0A1C15.toInt(), 0xFF26473C.toInt(), 0xFF1B3A2F.toInt(), 0x09FFFFFF, 0xFFD6B36A.toInt(),
            0xFFFFFFFF.toInt(), 0xB3FFFFFF.toInt(), 0xFFD6B36A.toInt(), 0xFF15281F.toInt()),
        // Parchment
        Style(0xFFEFE6D2.toInt(), 0xFFFBF7EC.toInt(), 0xFFF6EFDD.toInt(), 0x14B08A3E, 0xFFB08A3E.toInt(),
            0xFF2A2116.toInt(), 0xB32A2116.toInt(), 0xFF1E6B52.toInt(), 0xFFFFFFFF.toInt()),
        // Night
        Style(0xFF050506.toInt(), 0xFF1C1C1E.toInt(), 0xFF141416.toInt(), 0x08FFFFFF, 0xFFD6B36A.toInt(),
            0xFFFFFFFF.toInt(), 0xA6EBEBF5.toInt(), 0xFFD6B36A.toInt(), 0xFF141416.toInt()),
        // Light — plain Apple grouped style
        Style(0xFFF2F2F7.toInt(), 0xFFFFFFFF.toInt(), 0xFFFFFFFF.toInt(), 0x00000000, 0x2E3C3C43,
            0xFF000000.toInt(), 0x993C3C43.toInt(), 0xFF1E6B52.toInt(), 0xFFFFFFFF.toInt()),
    )

    private const val W = 1080
    private const val H = 1350

    fun render(ctx: Context, z: Zikr, title: String, styleIndex: Int, lang: Lang, withMeaning: Boolean): Bitmap {
        val st = styles[styleIndex]
        val bmp = Bitmap.createBitmap(W, H, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        val amiri = ResourcesCompat.getFont(ctx, R.font.amiri_regular) ?: Typeface.SERIF
        val amiriBold = ResourcesCompat.getFont(ctx, R.font.amiri_bold) ?: amiri
        val ruqaa = ResourcesCompat.getFont(ctx, R.font.aref_ruqaa_bold) ?: amiriBold
        val inter = ResourcesCompat.getFont(ctx, R.font.inter) ?: Typeface.DEFAULT
        val serif = Typeface.create(Typeface.SERIF, Typeface.NORMAL)
        val serifItalic = Typeface.create(Typeface.SERIF, Typeface.ITALIC)
        val p = Paint(Paint.ANTI_ALIAS_FLAG)

        // Background + faint interlaced-star pattern.
        c.drawColor(st.bg)
        if (st.pattern != 0) {
            p.style = Paint.Style.STROKE; p.strokeWidth = 2f; p.color = st.pattern
            val step = 150f
            var y = 0f
            while (y < H + step) {
                var x = 0f
                while (x < W + step) {
                    c.drawPath(star(x, y, step * 0.5f, 0.78f), p)
                    c.drawRect(x - step * 0.18f, y - step * 0.18f, x + step * 0.18f, y + step * 0.18f, p)
                    x += step
                }
                y += step
            }
        }

        // Header: chapter title over a gold rule with a rosette.
        val titlePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = if (lang == Lang.AR) amiriBold else inter
            if (lang != Lang.AR) fontVariationSettings = "'wght' 600"
            textSize = if (lang == Lang.AR) 42f else 32f
            color = st.text2
        }
        drawCentered(c, ellipsize(title, titlePaint, 880f), titlePaint, 120f)
        ornament(c, p, 168f, st.frame, 150f)

        // The card with the Mushaf's double gold frame.
        val card = RectF(70f, 220f, W - 70f, H - 250f)
        p.style = Paint.Style.FILL
        p.color = 0xFFFFFFFF.toInt()
        p.shader = LinearGradient(0f, card.top, 0f, card.bottom, st.cardTop, st.cardBottom, Shader.TileMode.CLAMP)
        c.drawRoundRect(card, 60f, 60f, p)
        p.shader = null
        p.style = Paint.Style.STROKE
        p.color = withAlpha(st.frame, if (styleIndex == 3) 255 else 150); p.strokeWidth = 2.5f
        c.drawRoundRect(card, 60f, 60f, p)
        if (styleIndex != 3) {
            val inner = RectF(card.left + 14f, card.top + 14f, card.right - 14f, card.bottom - 14f)
            p.color = withAlpha(st.frame, 60); p.strokeWidth = 1.5f
            c.drawRoundRect(inner, 48f, 48f, p)
        }

        // Arabic auto-fitted, then the translation.
        val meaning = if (withMeaning) z.meaning(lang).trim() else ""
        val innerW = (card.width() - 140f).toInt()
        val innerH = card.height() - 140f - if (z.repeat > 1) 80f else 0f
        val arPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply { typeface = amiri; color = st.text }
        val mPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply { typeface = serifItalic; color = st.text2 }
        var arSize = 92f
        var arLayout: StaticLayout
        var mLayout: StaticLayout?
        while (true) {
            arPaint.textSize = arSize
            mPaint.textSize = (arSize * 0.42f).coerceIn(24f, 32f)
            arLayout = layout(z.ar, arPaint, innerW, rtl = true, spacing = 1.5f)
            mLayout = if (meaning.isNotEmpty()) layout(meaning, mPaint, innerW, rtl = false, spacing = 1.3f) else null
            val total = arLayout.height + (mLayout?.let { it.height + 80 } ?: 0)
            if (total <= innerH || arSize <= 28f) break
            arSize -= 2f
        }
        val total = arLayout.height + (mLayout?.let { it.height + 80 } ?: 0)
        var top = card.top + 70f + ((innerH - total) / 2f).coerceAtLeast(0f)
        c.save(); c.translate(card.left + 70f, top); arLayout.draw(c); c.restore()
        top += arLayout.height
        mLayout?.let {
            ornament(c, p, top + 40f, st.frame, 80f)
            c.save(); c.translate(card.left + 70f, top + 80f); it.draw(c); c.restore()
        }
        if (z.repeat > 1) {
            val chip = TextPaint(Paint.ANTI_ALIAS_FLAG).apply { typeface = inter; fontVariationSettings = "'wght' 650"; textSize = 28f; color = st.accent }
            val label = "× ${z.repeat}"
            val tw = chip.measureText(label)
            val r = RectF(W / 2f - tw / 2 - 26f, card.bottom - 100f, W / 2f + tw / 2 + 26f, card.bottom - 50f)
            p.style = Paint.Style.FILL; p.color = withAlpha(st.accent, 38)
            c.drawRoundRect(r, 25f, 25f, p)
            drawCentered(c, label, chip, card.bottom - 64f)
        }

        // Signature: calligraphic name, rosette, Latin name.
        val ar = TextPaint(Paint.ANTI_ALIAS_FLAG).apply { typeface = ruqaa; textSize = 60f; color = st.accent }
        val en = TextPaint(Paint.ANTI_ALIAS_FLAG).apply { typeface = serif; textSize = 26f; color = st.text2; letterSpacing = 0.35f; isFakeBoldText = true }
        val y0 = H - 150f
        val arW = ar.measureText("أذكار إسلامية")
        val enW = en.measureText("ISLAMIC AZKAR")
        val gap = 60f
        val totalW = enW + gap + arW
        val startX = (W - totalW) / 2f
        c.drawText("ISLAMIC AZKAR", startX, y0, en)
        p.style = Paint.Style.FILL; p.color = st.frame
        c.drawPath(star(startX + enW + gap / 2f, y0 - 9f, 10f, 0.72f), p)
        c.drawText("أذكار إسلامية", startX + enW + gap, y0 + 6f, ar)
        val small = TextPaint(Paint.ANTI_ALIAS_FLAG).apply { typeface = inter; textSize = 22f; color = withAlpha(st.text2, 160) }
        drawCentered(c, "حصن المسلم  ·  Hisn Al Muslim", small, H - 82f)
        return bmp
    }

    private fun ornament(c: Canvas, p: Paint, y: Float, color: Int, half: Float) {
        p.shader = null
        p.color = 0xFFFFFFFF.toInt() // opaque, so the gradient isn't dimmed by an earlier alpha
        p.style = Paint.Style.STROKE; p.strokeWidth = 2f
        p.shader = LinearGradient(W / 2f - half - 20f, y, W / 2f - 20f, y, 0, color, Shader.TileMode.CLAMP)
        c.drawLine(W / 2f - half - 20f, y, W / 2f - 20f, y, p)
        p.shader = LinearGradient(W / 2f + 20f, y, W / 2f + half + 20f, y, color, 0, Shader.TileMode.CLAMP)
        c.drawLine(W / 2f + 20f, y, W / 2f + half + 20f, y, p)
        p.shader = null
        p.style = Paint.Style.FILL; p.color = color
        c.drawPath(star(W / 2f, y, 9f, 0.72f), p)
    }

    fun share(ctx: Context, bmp: Bitmap, caption: String) {
        val dir = File(ctx.cacheDir, "share").apply { mkdirs() }
        val file = File(dir, "islamic-azkar.png")
        file.outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
        val uri = FileProvider.getUriForFile(ctx, ctx.packageName + ".fileprovider", file)
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_TEXT, caption)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        ctx.startActivity(Intent.createChooser(send, "Islamic Azkar").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    private fun layout(text: String, paint: TextPaint, width: Int, rtl: Boolean, spacing: Float): StaticLayout =
        StaticLayout.Builder.obtain(text, 0, text.length, paint, width)
            .setAlignment(Layout.Alignment.ALIGN_CENTER)
            .setTextDirection(if (rtl) TextDirectionHeuristics.RTL else TextDirectionHeuristics.FIRSTSTRONG_LTR)
            .setLineSpacing(0f, spacing)
            .setIncludePad(true)
            .build()

    private fun drawCentered(c: Canvas, text: String, paint: TextPaint, baseline: Float) {
        c.drawText(text, (W - paint.measureText(text)) / 2f, baseline, paint)
    }

    private fun ellipsize(text: String, paint: TextPaint, max: Float): String {
        if (paint.measureText(text) <= max) return text
        var t = text
        while (t.isNotEmpty() && paint.measureText("$t…") > max) t = t.dropLast(1)
        return "$t…"
    }

    private fun star(cx: Float, cy: Float, r: Float, inner: Float = 0.62f): Path = Path().apply {
        for (i in 0 until 16) {
            val a = Math.PI / 8 * i - Math.PI / 2
            val rr = if (i % 2 == 0) r else r * inner
            val x = cx + (rr * cos(a)).toFloat()
            val y = cy + (rr * sin(a)).toFloat()
            if (i == 0) moveTo(x, y) else lineTo(x, y)
        }
        close()
    }

    private fun withAlpha(color: Int, alpha: Int): Int = (color and 0x00FFFFFF) or (alpha.coerceIn(0, 255) shl 24)

}
