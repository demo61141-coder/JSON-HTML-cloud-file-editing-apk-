package com.example.render

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.net.Uri
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import androidx.core.content.res.ResourcesCompat
import com.example.R
import com.example.model.DocumentaryScene
import com.example.model.OverlayType
import com.example.model.SceneAnimationStyle
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.sin

class SceneFrameRenderer(private val context: Context) {

    private val bengaliTypeface: Typeface by lazy {
        ResourcesCompat.getFont(context, R.font.noto_sans_bengali) ?: Typeface.DEFAULT_BOLD
    }
    private val displayTypeface: Typeface by lazy {
        ResourcesCompat.getFont(context, R.font.space_grotesk) ?: Typeface.DEFAULT_BOLD
    }
    private val monoTypeface: Typeface by lazy {
        ResourcesCompat.getFont(context, R.font.jetbrains_mono) ?: Typeface.MONOSPACE
    }

    private val bitmapCache = HashMap<String, Bitmap?>()
    private val resBitmapCache = HashMap<Int, Bitmap?>()

    private val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val overlayPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val bitmapPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)

    private val headlinePaint = TextPaint(Paint.ANTI_ALIAS_FLAG)
    private val subtextPaint = TextPaint(Paint.ANTI_ALIAS_FLAG)
    private val badgePaint = TextPaint(Paint.ANTI_ALIAS_FLAG)
    private val monoPaint = TextPaint(Paint.ANTI_ALIAS_FLAG)
    private val tickerPaint = TextPaint(Paint.ANTI_ALIAS_FLAG)

    fun drawFrame(
        canvas: Canvas,
        width: Int,
        height: Int,
        scene: DocumentaryScene,
        sceneIndex: Int,
        totalScenes: Int,
        sceneProgress: Float,
        globalTimeSec: Float,
        totalDurationSec: Float,
        channelName: String
    ) {
        if (width <= 0 || height <= 0) return
        val w = width.toFloat()
        val h = height.toFloat()
        val unit = h / 1920f
        val p = sceneProgress.coerceIn(0f, 1f)
        val easeOut = 1f - (1f - p).let { it * it * it }
        val entrance = (p / 0.22f).coerceIn(0f, 1f).let { 1f - (1f - it) * (1f - it) * (1f - it) }

        // 1. Background gradient
        val topColor = scene.palette.bgTopHex.toInt()
        val bottomColor = scene.palette.bgBottomHex.toInt()
        val accentColor = scene.palette.accentHex.toInt()
        val badgeBgColor = scene.palette.badgeBgHex.toInt()

        bgPaint.shader = LinearGradient(
            0f, 0f, 0f, h,
            intArrayOf(topColor, bottomColor, 0xFF04060B.toInt()),
            floatArrayOf(0f, 0.55f, 1f),
            Shader.TileMode.CLAMP
        )
        canvas.drawRect(0f, 0f, w, h, bgPaint)
        bgPaint.shader = null

        // 2. Background Photo (user gallery URI or built-in documentary backdrop) with Ken Burns motion
        val bgBitmap = loadSceneBackgroundBitmap(scene)
        if (bgBitmap != null && !bgBitmap.isRecycled) {
            drawKenBurnsBitmap(canvas, bgBitmap, w, h, p, scene.animationStyle)
            // Cinematic vignette & color wash over photo
            bgPaint.shader = LinearGradient(
                0f, 0f, 0f, h,
                intArrayOf(
                    applyAlpha(0xFF050811.toInt(), 0.58f),
                    applyAlpha(topColor, 0.42f),
                    applyAlpha(0xFF060912.toInt(), 0.86f),
                    applyAlpha(0xFF030509.toInt(), 0.96f)
                ),
                floatArrayOf(0f, 0.35f, 0.68f, 1f),
                Shader.TileMode.CLAMP
            )
            canvas.drawRect(0f, 0f, w, h, bgPaint)
            bgPaint.shader = null
        } else {
            // Atmospheric studio radial glow
            bgPaint.shader = RadialGradient(
                w * (0.28f + 0.15f * sin(globalTimeSec * 0.7f)),
                h * 0.28f,
                w * 0.95f,
                applyAlpha(accentColor, 0.26f),
                Color.TRANSPARENT,
                Shader.TileMode.CLAMP
            )
            canvas.drawRect(0f, 0f, w, h, bgPaint)
            bgPaint.shader = null
        }

        // Subtle broadcast grid & scan beam
        linePaint.color = applyAlpha(Color.WHITE, 0.045f)
        linePaint.strokeWidth = (1.5f * unit).coerceAtLeast(1f)
        val gridStep = 120f * unit
        var gx = gridStep
        while (gx < w) {
            canvas.drawLine(gx, 0f, gx, h, linePaint)
            gx += gridStep
        }
        var gy = gridStep
        while (gy < h) {
            canvas.drawLine(0f, gy, w, gy, linePaint)
            gy += gridStep
        }

        // Animated pulse ring for PULSE_ALERT / ACC RAID
        if (scene.animationStyle == SceneAnimationStyle.PULSE_ALERT) {
            val pulseWave = ((globalTimeSec * 1.4f) % 1f)
            overlayPaint.style = Paint.Style.STROKE
            overlayPaint.strokeWidth = 4f * unit
            overlayPaint.color = applyAlpha(accentColor, (1f - pulseWave) * 0.45f)
            canvas.drawCircle(w * 0.5f, h * 0.34f, (110f + 260f * pulseWave) * unit, overlayPaint)
            overlayPaint.style = Paint.Style.FILL
        }

        // 3. Top Broadcast HUD
        val marginX = 56f * unit
        val topHudY = 74f * unit

        // LIVE REC Pill
        val livePillRect = RectF(marginX, topHudY, marginX + 218f * unit, topHudY + 56f * unit)
        overlayPaint.color = applyAlpha(0xFF0A0F1C.toInt(), 0.85f)
        canvas.drawRoundRect(livePillRect, 28f * unit, 28f * unit, overlayPaint)
        overlayPaint.style = Paint.Style.STROKE
        overlayPaint.strokeWidth = 2f * unit
        overlayPaint.color = applyAlpha(accentColor, 0.7f)
        canvas.drawRoundRect(livePillRect, 28f * unit, 28f * unit, overlayPaint)
        overlayPaint.style = Paint.Style.FILL

        val blinkAlpha = 0.45f + 0.55f * ((sin(globalTimeSec * 6.0f) + 1f) * 0.5f)
        overlayPaint.color = applyAlpha(0xFFFF3B30.toInt(), blinkAlpha)
        canvas.drawCircle(marginX + 34f * unit, topHudY + 28f * unit, 11f * unit, overlayPaint)

        monoPaint.typeface = displayTypeface
        monoPaint.color = Color.WHITE
        monoPaint.textSize = 26f * unit
        monoPaint.isFakeBoldText = true
        canvas.drawText("LIVE • REC", marginX + 60f * unit, topHudY + 37f * unit, monoPaint)

        // Scene Index + Timecode Pill on top right
        val sceneCode = String.format("%02d/%02d  %s", sceneIndex + 1, totalScenes, formatTime(globalTimeSec))
        monoPaint.typeface = monoTypeface
        monoPaint.textSize = 25f * unit
        val codeW = monoPaint.measureText(sceneCode)
        val rightPillRect = RectF(w - marginX - codeW - 44f * unit, topHudY, w - marginX, topHudY + 56f * unit)
        overlayPaint.color = applyAlpha(0xFF0A0F1C.toInt(), 0.85f)
        canvas.drawRoundRect(rightPillRect, 28f * unit, 28f * unit, overlayPaint)
        monoPaint.color = 0xFFE2E8F0.toInt()
        canvas.drawText(sceneCode, rightPillRect.left + 22f * unit, topHudY + 37f * unit, monoPaint)

        // Channel Watermark below LIVE pill
        monoPaint.typeface = displayTypeface
        monoPaint.textSize = 24f * unit
        monoPaint.color = applyAlpha(0xFFCBD5E1.toInt(), 0.85f)
        canvas.drawText(channelName.uppercase(), marginX + 6f * unit, topHudY + 94f * unit, monoPaint)

        // 4. Story Card (Middle-Lower Area)
        val slideOffsetY = when (scene.animationStyle) {
            SceneAnimationStyle.BREAKING_SLIDE -> (1f - entrance) * 90f * unit
            SceneAnimationStyle.DOSSIER_REVEAL -> (1f - entrance) * 55f * unit
            else -> (1f - entrance) * 40f * unit
        }

        val cardLeft = marginX
        val cardRight = w - marginX
        val cardWidth = (cardRight - cardLeft).toInt().coerceAtLeast(100)
        val cardBottom = h - 250f * unit

        // Measure Headline and Subtext using StaticLayout
        headlinePaint.typeface = bengaliTypeface
        headlinePaint.textSize = 54f * unit
        headlinePaint.color = Color.WHITE
        headlinePaint.isFakeBoldText = true
        headlinePaint.setShadowLayer(8f * unit, 0f, 3f * unit, applyAlpha(Color.BLACK, 0.7f))

        val visibleHeadline = if (scene.animationStyle == SceneAnimationStyle.TYPEWRITER_DOC) {
            val count = ((scene.headline.length) * (p / 0.45f).coerceIn(0.12f, 1f)).toInt()
            scene.headline.take(count.coerceAtLeast(1))
        } else {
            scene.headline
        }

        val innerPad = 40f * unit
        val textContentWidth = (cardWidth - (innerPad * 2)).toInt().coerceAtLeast(60)
        val headlineLayout = StaticLayout.Builder
            .obtain(visibleHeadline, 0, visibleHeadline.length, headlinePaint, textContentWidth)
            .setAlignment(Layout.Alignment.ALIGN_NORMAL)
            .setLineSpacing(6f * unit, 1.08f)
            .build()

        subtextPaint.typeface = bengaliTypeface
        subtextPaint.textSize = 33f * unit
        subtextPaint.color = 0xFFE2E8F0.toInt()
        subtextPaint.clearShadowLayer()

        val subtextLayout = StaticLayout.Builder
            .obtain(scene.subtext, 0, scene.subtext.length, subtextPaint, textContentWidth)
            .setAlignment(Layout.Alignment.ALIGN_NORMAL)
            .setLineSpacing(5f * unit, 1.15f)
            .build()

        val badgeH = 52f * unit
        val reporterH = 44f * unit
        val cardHeight = (innerPad * 2) + badgeH + (26f * unit) + headlineLayout.height + (24f * unit) + subtextLayout.height + (26f * unit) + reporterH
        val cardTop = (cardBottom - cardHeight + slideOffsetY).coerceAtLeast(h * 0.25f)
        val cardRect = RectF(cardLeft, cardTop, cardRight, cardTop + cardHeight)

        // Frosted obsidian dossier card
        overlayPaint.color = applyAlpha(0xFF0A0F1B.toInt(), 0.84f * entrance)
        canvas.drawRoundRect(cardRect, 28f * unit, 28f * unit, overlayPaint)

        // Card border + left crimson/amber vertical accent stripe
        overlayPaint.style = Paint.Style.STROKE
        overlayPaint.strokeWidth = 2.5f * unit
        overlayPaint.color = applyAlpha(accentColor, 0.45f * entrance)
        canvas.drawRoundRect(cardRect, 28f * unit, 28f * unit, overlayPaint)
        overlayPaint.style = Paint.Style.FILL

        val stripeRect = RectF(cardLeft, cardTop + 26f * unit, cardLeft + 10f * unit, cardRect.bottom - 26f * unit)
        overlayPaint.color = applyAlpha(accentColor, entrance)
        canvas.drawRoundRect(stripeRect, 5f * unit, 5f * unit, overlayPaint)

        // Badge pill inside card
        badgePaint.typeface = bengaliTypeface
        badgePaint.textSize = 27f * unit
        badgePaint.color = Color.WHITE
        badgePaint.isFakeBoldText = true
        val badgeTextW = badgePaint.measureText(scene.badge)
        val badgeRect = RectF(
            cardLeft + innerPad,
            cardTop + innerPad,
            min(cardRight - innerPad, cardLeft + innerPad + badgeTextW + 44f * unit),
            cardTop + innerPad + badgeH
        )
        overlayPaint.color = applyAlpha(badgeBgColor, entrance)
        canvas.drawRoundRect(badgeRect, 14f * unit, 14f * unit, overlayPaint)
        canvas.drawText(
            scene.badge,
            badgeRect.left + 22f * unit,
            badgeRect.top + 35f * unit,
            badgePaint
        )

        // Draw Headline
        val headlineY = badgeRect.bottom + 26f * unit
        canvas.save()
        canvas.translate(cardLeft + innerPad, headlineY)
        headlinePaint.alpha = (255 * entrance).toInt()
        headlineLayout.draw(canvas)
        canvas.restore()

        // Divider bar
        val dividerY = headlineY + headlineLayout.height + 14f * unit
        overlayPaint.color = applyAlpha(accentColor, 0.85f * entrance)
        canvas.drawRoundRect(
            RectF(cardLeft + innerPad, dividerY, cardLeft + innerPad + 140f * unit * easeOut, dividerY + 5f * unit),
            3f * unit, 3f * unit, overlayPaint
        )

        // Draw Subtext
        val subtextY = dividerY + 20f * unit
        canvas.save()
        canvas.translate(cardLeft + innerPad, subtextY)
        subtextPaint.alpha = (242 * entrance).toInt()
        subtextLayout.draw(canvas)
        canvas.restore()

        // Reporter Tag Footer inside card
        val reporterY = subtextY + subtextLayout.height + 34f * unit
        badgePaint.typeface = bengaliTypeface
        badgePaint.textSize = 25f * unit
        badgePaint.color = applyAlpha(0xFFFBBF24.toInt(), entrance)
        canvas.drawText("🎙  ${scene.reporterTag}", cardLeft + innerPad, reporterY, badgePaint)

        // 5. Custom Overlay Elements (Draggable Text Badges & Gallery Photos/Logos)
        for (item in scene.overlays) {
            val cx = item.xFraction * w
            val cy = item.yFraction * h
            val s = item.scale * unit

            when (item.type) {
                OverlayType.IMAGE_STICKER -> {
                    val bmp = item.imageUri?.let { loadUriBitmap(it) }
                    if (bmp != null && !bmp.isRecycled) {
                        val targetSize = 180f * s
                        val aspect = bmp.width.toFloat() / bmp.height.coerceAtLeast(1)
                        val drawW = if (aspect >= 1f) targetSize else targetSize * aspect
                        val drawH = if (aspect >= 1f) targetSize / aspect else targetSize
                        val rect = RectF(cx - drawW / 2f, cy - drawH / 2f, cx + drawW / 2f, cy + drawH / 2f)

                        canvas.save()
                        val clipPath = Path().apply {
                            addRoundRect(rect, 22f * s, 22f * s, Path.Direction.CW)
                        }
                        canvas.clipPath(clipPath)
                        canvas.drawBitmap(bmp, null, rect, bitmapPaint)
                        canvas.restore()

                        overlayPaint.style = Paint.Style.STROKE
                        overlayPaint.strokeWidth = 3f * unit
                        overlayPaint.color = Color.WHITE
                        canvas.drawRoundRect(rect, 22f * s, 22f * s, overlayPaint)
                        overlayPaint.style = Paint.Style.FILL
                    }
                }
                OverlayType.TEXT_BADGE, OverlayType.LOWER_THIRD -> {
                    val label = item.text.ifBlank { "BREAKING" }
                    badgePaint.typeface = bengaliTypeface
                    badgePaint.textSize = 28f * s
                    badgePaint.isFakeBoldText = true
                    val tw = badgePaint.measureText(label)
                    val padH = 26f * s
                    val boxH = 54f * s
                    val rect = RectF(
                        cx - (tw / 2f) - padH,
                        cy - boxH / 2f,
                        cx + (tw / 2f) + padH,
                        cy + boxH / 2f
                    )
                    overlayPaint.color = applyAlpha(item.colorHex.toInt(), 0.92f)
                    canvas.drawRoundRect(rect, 14f * s, 14f * s, overlayPaint)
                    badgePaint.color = Color.WHITE
                    canvas.drawText(label, rect.left + padH, rect.bottom - 16f * s, badgePaint)
                }
            }
        }

        // 6. Audio Spectrum Equalizer Bars (above bottom ticker)
        val eqBaseY = h - 186f * unit
        val barCount = 18
        val barW = 7f * unit
        val barGap = 8f * unit
        for (b in 0 until barCount) {
            val phase = globalTimeSec * 7.5f + b * 0.65f
            val barH = (12f + 28f * abs(sin(phase))) * unit
            val bx = marginX + b * (barW + barGap)
            overlayPaint.color = applyAlpha(accentColor, 0.85f)
            canvas.drawRoundRect(
                RectF(bx, eqBaseY - barH, bx + barW, eqBaseY),
                3f * unit, 3f * unit, overlayPaint
            )
        }

        // 7. Bottom Breaking News Ticker Bar
        val tickerTop = h - 164f * unit
        val tickerBottom = h - 78f * unit
        overlayPaint.color = 0xFFD32F2F.toInt()
        canvas.drawRect(0f, tickerTop, w, tickerBottom, overlayPaint)

        // Left "NEWS" tag box on ticker
        val tagW = 190f * unit
        overlayPaint.color = 0xFF0A0F1B.toInt()
        canvas.drawRect(0f, tickerTop, tagW, tickerBottom, overlayPaint)
        badgePaint.typeface = displayTypeface
        badgePaint.textSize = 26f * unit
        badgePaint.color = 0xFFFBBF24.toInt()
        canvas.drawText("সংবাদ ডেস্ক", 24f * unit, tickerTop + 53f * unit, badgePaint)

        // Scrolling ticker text
        val tickerMsg = "${scene.tickerText}   ★   ${scene.tickerText}"
        tickerPaint.typeface = bengaliTypeface
        tickerPaint.textSize = 31f * unit
        tickerPaint.color = Color.WHITE
        val msgWidth = tickerPaint.measureText(scene.tickerText + "   ★   ").coerceAtLeast(1f)
        val scrollOffset = (globalTimeSec * 140f * unit) % msgWidth

        canvas.save()
        canvas.clipRect(tagW + 8f * unit, tickerTop, w, tickerBottom)
        canvas.drawText(tickerMsg, tagW + 24f * unit - scrollOffset, tickerTop + 55f * unit, tickerPaint)
        canvas.restore()

        // 8. Global Documentary Timeline Progress Bar at the very bottom
        val progTop = h - 22f * unit
        overlayPaint.color = applyAlpha(Color.WHITE, 0.18f)
        canvas.drawRect(0f, progTop, w, h, overlayPaint)
        val globalProgress = if (totalDurationSec > 0f) (globalTimeSec / totalDurationSec).coerceIn(0f, 1f) else 0f
        overlayPaint.color = 0xFFF59E0B.toInt()
        canvas.drawRect(0f, progTop, w * globalProgress, h, overlayPaint)
    }

    private fun drawKenBurnsBitmap(
        canvas: Canvas,
        bitmap: Bitmap,
        targetW: Float,
        targetH: Float,
        progress: Float,
        animStyle: SceneAnimationStyle
    ) {
        val bw = bitmap.width.toFloat().coerceAtLeast(1f)
        val bh = bitmap.height.toFloat().coerceAtLeast(1f)
        val baseScale = maxOf(targetW / bw, targetH / bh)
        val zoom = when (animStyle) {
            SceneAnimationStyle.KEN_BURNS_ZOOM -> 1.0f + 0.14f * progress
            SceneAnimationStyle.PULSE_ALERT -> 1.04f + 0.08f * sin(progress * PI.toFloat())
            else -> 1.02f + 0.07f * progress
        }
        val finalScale = baseScale * zoom
        val scaledW = bw * finalScale
        val scaledH = bh * finalScale
        val dx = (targetW - scaledW) * 0.5f + ((progress - 0.5f) * 28f * (targetW / 1080f))
        val dy = (targetH - scaledH) * 0.5f

        val matrix = Matrix().apply {
            postScale(finalScale, finalScale)
            postTranslate(dx, dy)
        }
        canvas.drawBitmap(bitmap, matrix, bitmapPaint)
    }

    private fun loadSceneBackgroundBitmap(scene: DocumentaryScene): Bitmap? {
        scene.backgroundImageUri?.let { uriStr ->
            loadUriBitmap(uriStr)?.let { return it }
        }
        val resId = scene.backgroundResId ?: return null
        return resBitmapCache.getOrPut(resId) {
            try {
                val opts = BitmapFactory.Options().apply { inSampleSize = 1 }
                BitmapFactory.decodeResource(context.resources, resId, opts)
            } catch (_: Exception) {
                null
            }
        }
    }

    private fun loadUriBitmap(uriStr: String): Bitmap? {
        return bitmapCache.getOrPut(uriStr) {
            try {
                val uri = Uri.parse(uriStr)
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    val opts = BitmapFactory.Options().apply {
                        inJustDecodeBounds = true
                    }
                    BitmapFactory.decodeStream(stream, null, opts)
                    val maxDim = maxOf(opts.outWidth, opts.outHeight)
                    var sample = 1
                    while (maxDim / sample > 1440) {
                        sample *= 2
                    }
                    context.contentResolver.openInputStream(uri)?.use { stream2 ->
                        val decodeOpts = BitmapFactory.Options().apply { inSampleSize = sample }
                        BitmapFactory.decodeStream(stream2, null, decodeOpts)
                    }
                }
            } catch (_: Exception) {
                null
            }
        }
    }

    private fun applyAlpha(color: Int, alphaFraction: Float): Int {
        val a = (Color.alpha(color) * alphaFraction.coerceIn(0f, 1f)).toInt().coerceIn(0, 255)
        return Color.argb(a, Color.red(color), Color.green(color), Color.blue(color))
    }

    private fun formatTime(sec: Float): String {
        val total = sec.coerceAtLeast(0f).toInt()
        val m = total / 60
        val s = total % 60
        return String.format("%02d:%02d", m, s)
    }
}
