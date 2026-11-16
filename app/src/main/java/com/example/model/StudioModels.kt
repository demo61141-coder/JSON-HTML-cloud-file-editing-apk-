package com.example.model

import androidx.annotation.DrawableRes
import com.example.R
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

enum class ResolutionPreset(
    val label: String,
    val width: Int,
    val height: Int,
    val bitrateBps: Int
) {
    P240("240p", 240, 426, 650_000),
    P360("360p", 360, 640, 1_200_000),
    P480("480p", 480, 854, 2_200_000),
    P720("720p", 720, 1280, 4_500_000),
    P1080("1080p", 1080, 1920, 8_500_000);

    val dimensionsText: String get() = "${width}×${height} (9:16)"
}

enum class SceneAnimationStyle(val labelBn: String, val labelEn: String) {
    KEN_BURNS_ZOOM("কেন বার্নস জুম", "Ken Burns"),
    BREAKING_SLIDE("ব্রেকিং স্লাইড", "Breaking Slide"),
    DOSSIER_REVEAL("ডসিয়ার রিভিল", "Dossier Reveal"),
    PULSE_ALERT("রেইড অ্যালার্ট", "Pulse Alert"),
    TYPEWRITER_DOC("টাইপরাইটার", "Typewriter")
}

enum class SceneThemePalette(
    val displayName: String,
    val bgTopHex: Long,
    val bgBottomHex: Long,
    val accentHex: Long,
    val badgeBgHex: Long
) {
    BREAKING_CRIMSON("Breaking Red", 0xFF28080D, 0xFF090C15, 0xFFE53935, 0xFFB71C1C),
    ACC_RAID_NOIR("ACC Raid Noir", 0xFF0E1B2A, 0xFF060911, 0xFFF59E0B, 0xFFD97706),
    EVIDENCE_AMBER("Dossier Gold", 0xFF20180B, 0xFF0A0C12, 0xFFFBBF24, 0xFFB45309),
    CIVIC_TEAL("Civic Report", 0xFF092229, 0xFF070B12, 0xFF06B6D4, 0xFF0E7490),
    OUTRO_OBSIDIAN("Channel Outro", 0xFF14182B, 0xFF06080F, 0xFFEF4444, 0xFF991B1B)
}

enum class OverlayType(val label: String) {
    TEXT_BADGE("Text Stamp"),
    IMAGE_STICKER("Photo / Logo"),
    LOWER_THIRD("Location Tag")
}

data class OverlayElement(
    val id: String = UUID.randomUUID().toString(),
    val type: OverlayType,
    val text: String = "",
    val imageUri: String? = null,
    val xFraction: Float = 0.5f,
    val yFraction: Float = 0.25f,
    val scale: Float = 1.0f,
    val colorHex: Long = 0xFFF59E0B
)

data class DocumentaryScene(
    val id: String = UUID.randomUUID().toString(),
    val badge: String,
    val headline: String,
    val subtext: String,
    val tickerText: String,
    val reporterTag: String = "রিপোর্টার হাসান • বিশেষ প্রতিবেদন",
    val durationSec: Float = 5.5f,
    val animationStyle: SceneAnimationStyle = SceneAnimationStyle.BREAKING_SLIDE,
    val palette: SceneThemePalette = SceneThemePalette.BREAKING_CRIMSON,
    @DrawableRes val backgroundResId: Int? = null,
    val backgroundImageUri: String? = null,
    val overlays: List<OverlayElement> = emptyList()
)

object DefaultDocumentaryProvider {
    const val DEFAULT_PROJECT_TITLE = "ডামুড্যা ভূমি অফিসে দুদকের অভিযান"
    const val DEFAULT_CHANNEL_NAME = "HASAN INVESTIGATES • LIVE"

    fun createReporterHasanScenes(): List<DocumentaryScene> = listOf(
        DocumentaryScene(
            badge = "ব্রেকিং নিউজ • শরীয়তপুর",
            headline = "ডামুড্যা ভূমি অফিসে দুদকের চাঞ্চল্যকর অভিযান",
            subtext = "নামজারি ও পর্চার নামে দীর্ঘদিনের ঘুষ বাণিজ্যের অভিযোগে শরীয়তপুরের ডামুড্যা উপজেলা ভূমি অফিসে আকস্মিক অভিযান পরিচালনা করেছে দুর্নীতি দমন কমিশন (দুদক)।",
            tickerText = "ব্রেকিং: শরীয়তপুরের ডামুড্যা ভূমি অফিসে দুদকের ফাঁদ • ঘুষের টাকাসহ হাতেনাতে আটক • রিপোর্টার হাসানের বিশেষ অনুসন্ধান",
            reporterTag = "রিপোর্টার হাসান • শরীয়তপুর ব্যুরো",
            durationSec = 6.0f,
            animationStyle = SceneAnimationStyle.BREAKING_SLIDE,
            palette = SceneThemePalette.BREAKING_CRIMSON,
            backgroundResId = R.drawable.img_doc_land_office_1794844915154,
            overlays = listOf(
                OverlayElement(
                    type = OverlayType.LOWER_THIRD,
                    text = "ডামুড্যা, শরীয়তপুর",
                    xFraction = 0.24f,
                    yFraction = 0.17f,
                    scale = 1.0f,
                    colorHex = 0xFFE53935
                )
            )
        ),
        DocumentaryScene(
            badge = "অনুসন্ধানী ডসিয়ার",
            headline = "সেবার বদলে প্রকাশ্যে চলছিল ঘুষের দরকষাকষি",
            subtext = "স্থানীয় ভুক্তভোগীদের অভিযোগ—সরকারি ফি'র বাইরে প্রতিটি নামজারি ও খতিয়ান সংশোধনে ৫ থেকে ২৫ হাজার টাকা পর্যন্ত আদায় করত একটি সিন্ডিকেট।",
            tickerText = "অনুসন্ধান: সাধারণ কৃষক ও জমির মালিকরা দিনের পর দিন হয়রানির শিকার হচ্ছিলেন • গোপন তথ্যে মাঠে নামে দুদক",
            reporterTag = "রিপোর্টার হাসান • তথ্যচিত্র",
            durationSec = 6.0f,
            animationStyle = SceneAnimationStyle.DOSSIER_REVEAL,
            palette = SceneThemePalette.EVIDENCE_AMBER,
            backgroundResId = R.drawable.img_doc_raid_dossier_1794844917233,
            overlays = listOf(
                OverlayElement(
                    type = OverlayType.TEXT_BADGE,
                    text = "গোপন অভিযোগ",
                    xFraction = 0.72f,
                    yFraction = 0.20f,
                    scale = 0.95f,
                    colorHex = 0xFFF59E0B
                )
            )
        ),
        DocumentaryScene(
            badge = "দুদকের ফাঁদ • লাইভ অ্যালার্ট",
            headline = "ঘুষের নগদ অর্থসহ হাতেনাতে ধরা পড়ল অভিযুক্তরা",
            subtext = "ভুক্তভোগীর কাছ থেকে চিহ্নিত নোট গ্রহণের মুহূর্তেই দুদকের সমন্বিত জেলা কার্যালয়ের বিশেষ টিম অফিস কক্ষ ঘেরাও করে নগদ অর্থ উদ্ধার করে।",
            tickerText = "দুদক অভিযান: চিহ্নিত টাকার নোটসহ হাতেনাতে আটক • ঘটনাস্থলে উত্তেজনা ও সাধারণ মানুষের ভিড়",
            reporterTag = "রিপোর্টার হাসান • ক্রাইম ডেস্ক",
            durationSec = 6.0f,
            animationStyle = SceneAnimationStyle.PULSE_ALERT,
            palette = SceneThemePalette.ACC_RAID_NOIR,
            backgroundResId = R.drawable.img_doc_land_office_1794844915154,
            overlays = listOf(
                OverlayElement(
                    type = OverlayType.TEXT_BADGE,
                    text = "ACC RAID",
                    xFraction = 0.76f,
                    yFraction = 0.19f,
                    scale = 1.0f,
                    colorHex = 0xFFEF4444
                )
            )
        ),
        DocumentaryScene(
            badge = "আলামত ও নথিপত্র জব্দ",
            headline = "সন্দেহভাজন রেজিস্টার, ফাইল ও মোবাইল জব্দ",
            subtext = "অভিযান চলাকালে বেশ কিছু অনিয়মিত নামজারি নথি, নগদ টাকা, এবং দালাল চক্রের সাথে যোগাযোগের ডিজিটাল প্রমাণ জব্দ করে তদন্ত দল।",
            tickerText = "তদন্ত আপডেট: জব্দকৃত নথিপত্র যাচাই-বাছাই করছে দুদক • জড়িত সবার বিরুদ্ধে মামলার প্রস্তুতি",
            reporterTag = "রিপোর্টার হাসান • আলামত বিশ্লেষণ",
            durationSec = 5.5f,
            animationStyle = SceneAnimationStyle.TYPEWRITER_DOC,
            palette = SceneThemePalette.EVIDENCE_AMBER,
            backgroundResId = R.drawable.img_doc_raid_dossier_1794844917233
        ),
        DocumentaryScene(
            badge = "জনগণের প্রতিক্রিয়া",
            headline = "দালালমুক্ত ভূমি অফিসের দাবি এলাকাবাসীর",
            subtext = "দুদকের এই পদক্ষেপে স্বস্তি প্রকাশ করেছেন ডামুড্যার সাধারণ মানুষ। তাদের দাবি—নিয়মিত নজরদারি ও দৃষ্টান্তমূলক শাস্তি নিশ্চিত করা হোক।",
            tickerText = "জনমত: শরীয়তপুরের প্রতিটি সরকারি দপ্তরে এমন শুদ্ধি অভিযান অব্যাহত রাখার দাবি স্থানীয়দের",
            reporterTag = "রিপোর্টার হাসান • মাঠ পর্যায়",
            durationSec = 5.5f,
            animationStyle = SceneAnimationStyle.KEN_BURNS_ZOOM,
            palette = SceneThemePalette.CIVIC_TEAL,
            backgroundResId = R.drawable.img_doc_land_office_1794844915154
        ),
        DocumentaryScene(
            badge = "বিশেষ প্রতিবেদন সমাপনী",
            headline = "দুর্নীতির বিরুদ্ধে অনুসন্ধান চলবেই",
            subtext = "জনস্বার্থে প্রতিটি অনিয়ম ও ঘুষ বাণিজ্যের মুখোশ উন্মোচনে আমাদের ক্যামেরা ও কলম সর্বদা সজাগ। পরবর্তী আপডেট জানতে সাথেই থাকুন।",
            tickerText = "রিপোর্টার হাসান • শরীয়তপুর বিশেষ ডকুমেন্টারি • সাবস্ক্রাইব ও শেয়ার করে দুর্নীতির বিরুদ্ধে সোচ্চার হোন",
            reporterTag = "রিপোর্টার হাসান • সমাপনী বক্তব্য",
            durationSec = 5.0f,
            animationStyle = SceneAnimationStyle.BREAKING_SLIDE,
            palette = SceneThemePalette.OUTRO_OBSIDIAN
        )
    )

    val SAMPLE_SCRIPT_TEMPLATE = """
        SCENE: ব্রেকিং নিউজ | শরীয়তপুরে দুদকের বিশেষ অভিযান
        শরীয়তপুরের ডামুড্যা ভূমি অফিসে ঘুষ বাণিজ্যের অভিযোগে অতর্কিত অভিযান চালিয়েছে দুদক।
        TICKER: ডামুড্যা ভূমি অফিসে দুদকের ফাঁদ • নগদ টাকাসহ আটক

        SCENE: অনুসন্ধান | ভুক্তভোগীদের দীর্ঘদিনের ভোগান্তি
        নামজারি ও পর্চার জন্য সাধারণ মানুষ বাধ্য হয়ে অতিরিক্ত টাকা দিচ্ছিলেন বলে অভিযোগ।
        TICKER: দালাল চক্রের দৌরাত্ম্য বন্ধের দাবি স্থানীয়দের

        SCENE: সমাপনী | রিপোর্টার হাসানের বিশেষ প্রতিবেদন
        জব্দকৃত নথিপত্র যাচাই শেষে আইনানুগ ব্যবস্থা নেওয়া হবে বলে জানিয়েছে দুদক।
        TICKER: দুর্নীতির বিরুদ্ধে আমাদের অনুসন্ধান অব্যাহত থাকবে
    """.trimIndent()
}

object ScriptParser {
    fun parseScriptToScenes(rawInput: String): List<DocumentaryScene> {
        val trimmed = rawInput.trim()
        if (trimmed.isEmpty()) return emptyList()

        // Try JSON format first
        if (trimmed.startsWith("[") || trimmed.startsWith("{")) {
            try {
                val jsonArray = if (trimmed.startsWith("[")) {
                    JSONArray(trimmed)
                } else {
                    val obj = JSONObject(trimmed)
                    obj.optJSONArray("scenes") ?: JSONArray().put(obj)
                }
                val parsed = mutableListOf<DocumentaryScene>()
                val palettes = SceneThemePalette.entries
                val anims = SceneAnimationStyle.entries
                for (i in 0 until jsonArray.length()) {
                    val item = jsonArray.optJSONObject(i) ?: continue
                    val headline = item.optString("headline").ifBlank {
                        item.optString("title", "সিন ${i + 1}")
                    }
                    val subtext = item.optString("subtext").ifBlank {
                        item.optString("body", item.optString("description", ""))
                    }
                    val badge = item.optString("badge").ifBlank { "বিশেষ প্রতিবেদন #${i + 1}" }
                    val ticker = item.optString("ticker").ifBlank { headline }
                    val duration = item.optDouble("duration", 5.5).toFloat().coerceIn(2f, 15f)
                    parsed.add(
                        DocumentaryScene(
                            badge = badge,
                            headline = headline,
                            subtext = subtext,
                            tickerText = ticker,
                            reporterTag = item.optString("reporter", "রিপোর্টার হাসান • বিশেষ প্রতিবেদন"),
                            durationSec = duration,
                            animationStyle = anims[i % anims.size],
                            palette = palettes[i % palettes.size]
                        )
                    )
                }
                if (parsed.isNotEmpty()) return parsed
            } catch (_: Exception) {
                // Fall back to text parser
            }
        }

        // Plain-text / SCENE: block / paragraph parser
        val blocks = if (trimmed.contains("SCENE:", ignoreCase = true)) {
            trimmed.split(Regex("(?=SCENE:)", RegexOption.IGNORE_CASE))
                .map { it.trim() }
                .filter { it.isNotEmpty() }
        } else if (trimmed.contains("---")) {
            trimmed.split("---").map { it.trim() }.filter { it.isNotEmpty() }
        } else {
            val paragraphs = trimmed.split(Regex("\\n\\s*\\n")).map { it.trim() }.filter { it.isNotEmpty() }
            if (paragraphs.size > 1) paragraphs
            else trimmed.lines().map { it.trim() }.filter { it.isNotEmpty() }
        }

        val palettes = SceneThemePalette.entries
        val anims = SceneAnimationStyle.entries

        return blocks.mapIndexed { idx, block ->
            val lines = block.lines().map { it.trim() }.filter { it.isNotEmpty() }.toMutableList()
            var badge = "দৃশ্য ${idx + 1} • প্রতিবেদন"
            var headline = "সংবাদ শিরোনাম"
            var ticker = ""
            val bodyLines = mutableListOf<String>()

            if (lines.isNotEmpty()) {
                val first = lines.removeAt(0)
                if (first.startsWith("SCENE:", ignoreCase = true)) {
                    val header = first.substringAfter(":").trim()
                    if (header.contains("|")) {
                        badge = header.substringBefore("|").trim().ifBlank { badge }
                        headline = header.substringAfter("|").trim().ifBlank { headline }
                    } else {
                        headline = header.ifBlank { headline }
                    }
                } else {
                    headline = first
                }
            }

            for (line in lines) {
                if (line.startsWith("TICKER:", ignoreCase = true)) {
                    ticker = line.substringAfter(":").trim()
                } else if (line.startsWith("BADGE:", ignoreCase = true)) {
                    badge = line.substringAfter(":").trim()
                } else {
                    bodyLines.add(line)
                }
            }

            val subtext = bodyLines.joinToString(" ").ifBlank {
                "রিপোর্টার হাসানের বিশেষ অনুসন্ধানী প্রতিবেদনের মূল অংশ।"
            }
            if (ticker.isBlank()) {
                ticker = "$badge • $headline"
            }

            DocumentaryScene(
                badge = badge,
                headline = headline,
                subtext = subtext,
                tickerText = ticker,
                reporterTag = "রিপোর্টার হাসান • নিউজ ডেস্ক",
                durationSec = (4.5f + (subtext.length / 60f)).coerceIn(4.0f, 9.0f),
                animationStyle = anims[idx % anims.size],
                palette = palettes[idx % palettes.size],
                backgroundResId = if (idx % 2 == 0) R.drawable.img_doc_land_office_1794844915154 else R.drawable.img_doc_raid_dossier_1794844917233
            )
        }
    }
}
