package com.example

import com.example.export.HtmlStudioExporter
import com.example.model.DefaultDocumentaryProvider
import com.example.model.ResolutionPreset
import com.example.model.ScriptParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class StudioModelTest {

    @Test
    fun reporterHasanDefaultScenes_arePreloaded() {
        val scenes = DefaultDocumentaryProvider.createReporterHasanScenes()
        assertEquals(6, scenes.size)
        assertTrue(scenes.first().headline.contains("ডামুড্যা"))
        assertTrue(scenes.any { it.tickerText.contains("দুদক") })
    }

    @Test
    fun scriptParser_parsesSampleBlocks() {
        val scenes = ScriptParser.parseScriptToScenes(DefaultDocumentaryProvider.SAMPLE_SCRIPT_TEMPLATE)
        assertEquals(3, scenes.size)
        assertEquals("ব্রেকিং নিউজ", scenes[0].badge)
        assertEquals("শরীয়তপুরে দুদকের বিশেষ অভিযান", scenes[0].headline)
    }

    @Test
    fun resolutionPresets_areVerticalAndEven() {
        val expected = listOf("240p", "360p", "480p", "720p", "1080p")
        assertEquals(expected, ResolutionPreset.entries.map { it.label })
        ResolutionPreset.entries.forEach { preset ->
            assertTrue("Width must be even for H.264", preset.width % 2 == 0)
            assertTrue("Height must be even for H.264", preset.height % 2 == 0)
            assertTrue("Must be vertical 9:16", preset.height > preset.width)
        }
    }

    @Test
    fun htmlExporter_containsCanvasAndMediaRecorder() {
        val scenes = DefaultDocumentaryProvider.createReporterHasanScenes()
        val html = HtmlStudioExporter.buildStandaloneHtml("Test Project", "CHANNEL", scenes)
        assertTrue(html.contains("<canvas id=\"previewCanvas\""))
        assertTrue(html.contains("MediaRecorder"))
        assertTrue(html.contains("ডামুড্যা"))
    }
}
