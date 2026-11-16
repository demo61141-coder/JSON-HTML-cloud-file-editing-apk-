package com.example.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.export.ExportResult
import com.example.export.VideoExporter
import com.example.model.DefaultDocumentaryProvider
import com.example.model.DocumentaryScene
import com.example.model.OverlayElement
import com.example.model.OverlayType
import com.example.model.ResolutionPreset
import com.example.model.SceneAnimationStyle
import com.example.model.SceneThemePalette
import com.example.model.ScriptParser
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

enum class StudioDestination {
    IMPORT_DASHBOARD,
    STUDIO_EDITOR
}

sealed class ExportState {
    data object Idle : ExportState()
    data class Exporting(
        val progress: Float,
        val currentFrame: Int,
        val totalFrames: Int,
        val resolution: ResolutionPreset
    ) : ExportState()
    data class Completed(val result: ExportResult) : ExportState()
    data class Error(val message: String) : ExportState()
}

data class StudioUiState(
    val destination: StudioDestination = StudioDestination.IMPORT_DASHBOARD,
    val projectTitle: String = DefaultDocumentaryProvider.DEFAULT_PROJECT_TITLE,
    val channelName: String = DefaultDocumentaryProvider.DEFAULT_CHANNEL_NAME,
    val scriptInput: String = DefaultDocumentaryProvider.SAMPLE_SCRIPT_TEMPLATE,
    val scenes: List<DocumentaryScene> = DefaultDocumentaryProvider.createReporterHasanScenes(),
    val selectedSceneIndex: Int = 0,
    val selectedOverlayId: String? = null,
    val currentTimeSec: Float = 0f,
    val isPlaying: Boolean = false,
    val isLooping: Boolean = true,
    val selectedResolution: ResolutionPreset = ResolutionPreset.P720,
    val selectedFps: Int = 24,
    val exportState: ExportState = ExportState.Idle,
    val showSceneEditSheet: Boolean = false,
    val showExportSheet: Boolean = false,
    val showAddTextOverlaySheet: Boolean = false,
    val statusBanner: String? = null
) {
    val totalDurationSec: Float
        get() = scenes.sumOf { it.durationSec.toDouble() }.toFloat().coerceAtLeast(1f)

    val currentScene: DocumentaryScene?
        get() = scenes.getOrNull(selectedSceneIndex)

    fun resolvePlaybackAt(timeSec: Float): Triple<Int, DocumentaryScene, Float> {
        if (scenes.isEmpty()) {
            val fallback = DefaultDocumentaryProvider.createReporterHasanScenes().first()
            return Triple(0, fallback, 0f)
        }
        var acc = 0f
        for (i in scenes.indices) {
            val s = scenes[i]
            val end = acc + s.durationSec
            if (timeSec <= end || i == scenes.lastIndex) {
                val p = ((timeSec - acc) / s.durationSec.coerceAtLeast(0.1f)).coerceIn(0f, 1f)
                return Triple(i, s, p)
            }
            acc = end
        }
        return Triple(0, scenes.first(), 0f)
    }

    fun sceneStartTime(index: Int): Float {
        var acc = 0f
        for (i in 0 until index.coerceAtMost(scenes.size)) {
            acc += scenes[i].durationSec
        }
        return acc
    }
}

class StudioViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(StudioUiState())
    val uiState: StateFlow<StudioUiState> = _uiState.asStateFlow()

    private val videoExporter = VideoExporter(application.applicationContext)
    private var playbackJob: Job? = null
    private var exportJob: Job? = null

    init {
        startPlaybackTicker()
    }

    private fun startPlaybackTicker() {
        playbackJob?.cancel()
        playbackJob = viewModelScope.launch {
            var lastTimeMs = System.currentTimeMillis()
            while (isActive) {
                delay(33L)
                val now = System.currentTimeMillis()
                val dt = ((now - lastTimeMs) / 1000f).coerceIn(0f, 0.1f)
                lastTimeMs = now

                val state = _uiState.value
                if (state.destination == StudioDestination.STUDIO_EDITOR && state.isPlaying && state.scenes.isNotEmpty()) {
                    val total = state.totalDurationSec
                    var nextTime = state.currentTimeSec + dt
                    var stillPlaying = true
                    if (nextTime >= total) {
                        if (state.isLooping) {
                            nextTime %= total
                        } else {
                            nextTime = total
                            stillPlaying = false
                        }
                    }
                    val (activeIdx, _, _) = state.resolvePlaybackAt(nextTime)
                    _uiState.update {
                        it.copy(
                            currentTimeSec = nextTime,
                            selectedSceneIndex = activeIdx,
                            isPlaying = stillPlaying
                        )
                    }
                }
            }
        }
    }

    fun updateScriptInput(text: String) {
        _uiState.update { it.copy(scriptInput = text) }
    }

    fun updateProjectMetadata(title: String, channelName: String) {
        _uiState.update {
            it.copy(
                projectTitle = title.ifBlank { DefaultDocumentaryProvider.DEFAULT_PROJECT_TITLE },
                channelName = channelName.ifBlank { DefaultDocumentaryProvider.DEFAULT_CHANNEL_NAME }
            )
        }
    }

    fun loadReporterHasanDefaultAndOpen() {
        _uiState.update {
            it.copy(
                projectTitle = DefaultDocumentaryProvider.DEFAULT_PROJECT_TITLE,
                channelName = DefaultDocumentaryProvider.DEFAULT_CHANNEL_NAME,
                scenes = DefaultDocumentaryProvider.createReporterHasanScenes(),
                selectedSceneIndex = 0,
                currentTimeSec = 0f,
                isPlaying = true,
                destination = StudioDestination.STUDIO_EDITOR,
                statusBanner = "রিপোর্টার হাসানের ডামুড্যা ডকুমেন্টারি লোড হয়েছে"
            )
        }
    }

    fun openStudioEditor() {
        _uiState.update {
            it.copy(
                destination = StudioDestination.STUDIO_EDITOR,
                isPlaying = true
            )
        }
    }

    fun goToImportDashboard() {
        _uiState.update {
            it.copy(
                destination = StudioDestination.IMPORT_DASHBOARD,
                isPlaying = false
            )
        }
    }

    fun importScriptAndOpen(rawScript: String, customTitle: String? = null) {
        val parsed = ScriptParser.parseScriptToScenes(rawScript)
        if (parsed.isEmpty()) {
            _uiState.update { it.copy(statusBanner = "স্ক্রিপ্ট খালি! অন্তত একটি লাইন লিখুন বা ফাইল বেছে নিন।") }
            return
        }
        _uiState.update {
            it.copy(
                projectTitle = customTitle?.takeIf { t -> t.isNotBlank() } ?: parsed.first().headline.take(36),
                scenes = parsed,
                selectedSceneIndex = 0,
                currentTimeSec = 0f,
                isPlaying = true,
                destination = StudioDestination.STUDIO_EDITOR,
                statusBanner = "${parsed.size}টি ভিজ্যুয়াল সিন তৈরি হয়েছে"
            )
        }
    }

    fun importFileFromUri(uri: Uri) {
        viewModelScope.launch {
            try {
                val content = getApplication<Application>().contentResolver
                    .openInputStream(uri)?.bufferedReader()?.use { it.readText() } ?: ""
                if (content.isBlank()) {
                    _uiState.update { it.copy(statusBanner = "ফাইলটি খালি বা পড়া যায়নি।") }
                } else {
                    _uiState.update { it.copy(scriptInput = content) }
                    importScriptAndOpen(content)
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(statusBanner = "ফাইল লোড করতে সমস্যা: ${e.message ?: "Error"}") }
            }
        }
    }

    fun togglePlayPause() {
        _uiState.update { state ->
            val restart = !state.isPlaying && state.currentTimeSec >= state.totalDurationSec - 0.05f
            state.copy(
                isPlaying = !state.isPlaying,
                currentTimeSec = if (restart) 0f else state.currentTimeSec
            )
        }
    }

    fun toggleLoop() {
        _uiState.update { it.copy(isLooping = !it.isLooping) }
    }

    fun seekTo(timeSec: Float) {
        _uiState.update { state ->
            val clamped = timeSec.coerceIn(0f, state.totalDurationSec)
            val (idx, _, _) = state.resolvePlaybackAt(clamped)
            state.copy(
                currentTimeSec = clamped,
                selectedSceneIndex = idx
            )
        }
    }

    fun selectSceneAndSeek(index: Int) {
        _uiState.update { state ->
            if (index !in state.scenes.indices) return@update state
            val startTime = state.sceneStartTime(index)
            state.copy(
                selectedSceneIndex = index,
                currentTimeSec = startTime + 0.35f.coerceAtMost(state.scenes[index].durationSec * 0.25f),
                selectedOverlayId = null
            )
        }
    }

    fun stepScene(delta: Int) {
        val state = _uiState.value
        if (state.scenes.isEmpty()) return
        val next = (state.selectedSceneIndex + delta).coerceIn(0, state.scenes.lastIndex)
        selectSceneAndSeek(next)
    }

    fun openSceneEditSheet(index: Int) {
        selectSceneAndSeek(index)
        _uiState.update { it.copy(isPlaying = false, showSceneEditSheet = true) }
    }

    fun closeSceneEditSheet() {
        _uiState.update { it.copy(showSceneEditSheet = false) }
    }

    fun updateScene(index: Int, updated: DocumentaryScene) {
        _uiState.update { state ->
            if (index !in state.scenes.indices) return@update state
            val list = state.scenes.toMutableList()
            list[index] = updated
            state.copy(scenes = list)
        }
    }

    fun addNewScene() {
        _uiState.update { state ->
            val idx = state.scenes.size + 1
            val newScene = DocumentaryScene(
                badge = "নতুন দৃশ্য #$idx",
                headline = "নতুন সংবাদ শিরোনাম লিখুন",
                subtext = "এখানে আপনার প্রতিবেদনের বিস্তারিত বিবরণ বা উদ্ধৃতি যুক্ত করুন।",
                tickerText = "ব্রেকিং আপডেট • দৃশ্য #$idx • রিপোর্টার হাসানের বিশেষ প্রতিবেদন",
                durationSec = 5.0f,
                animationStyle = SceneAnimationStyle.entries[idx % SceneAnimationStyle.entries.size],
                palette = SceneThemePalette.entries[idx % SceneThemePalette.entries.size]
            )
            val newList = state.scenes + newScene
            val newIndex = newList.lastIndex
            val startTime = state.totalDurationSec
            state.copy(
                scenes = newList,
                selectedSceneIndex = newIndex,
                currentTimeSec = startTime + 0.3f,
                isPlaying = false,
                showSceneEditSheet = true
            )
        }
    }

    fun duplicateScene(index: Int) {
        _uiState.update { state ->
            val src = state.scenes.getOrNull(index) ?: return@update state
            val copy = src.copy(
                id = java.util.UUID.randomUUID().toString(),
                badge = "${src.badge} (কপি)"
            )
            val mutable = state.scenes.toMutableList()
            mutable.add(index + 1, copy)
            state.copy(
                scenes = mutable,
                selectedSceneIndex = index + 1,
                statusBanner = "দৃশ্য ডুপ্লিকেট করা হয়েছে"
            )
        }
    }

    fun deleteScene(index: Int) {
        _uiState.update { state ->
            if (state.scenes.size <= 1) {
                return@update state.copy(statusBanner = "অন্তত ১টি দৃশ্য থাকতে হবে!")
            }
            val mutable = state.scenes.toMutableList()
            if (index !in mutable.indices) return@update state
            mutable.removeAt(index)
            val nextIdx = index.coerceAtMost(mutable.lastIndex)
            state.copy(
                scenes = mutable,
                selectedSceneIndex = nextIdx,
                currentTimeSec = 0f,
                showSceneEditSheet = false,
                statusBanner = "দৃশ্য ডিলিট করা হয়েছে"
            )
        }
    }

    fun moveScene(index: Int, delta: Int) {
        _uiState.update { state ->
            val target = index + delta
            if (index !in state.scenes.indices || target !in state.scenes.indices) return@update state
            val mutable = state.scenes.toMutableList()
            val item = mutable.removeAt(index)
            mutable.add(target, item)
            state.copy(
                scenes = mutable,
                selectedSceneIndex = target
            )
        }
    }

    fun setSceneBackgroundPhoto(index: Int, uriString: String?) {
        _uiState.update { state ->
            val scene = state.scenes.getOrNull(index) ?: return@update state
            val mutable = state.scenes.toMutableList()
            mutable[index] = scene.copy(
                backgroundImageUri = uriString,
                backgroundResId = if (uriString != null) null else scene.backgroundResId
            )
            state.copy(scenes = mutable, statusBanner = if (uriString != null) "গ্যালারি ছবি যুক্ত হয়েছে" else "ছবি সরানো হয়েছে")
        }
    }

    fun setSceneBuiltInBackdrop(index: Int, resId: Int?) {
        _uiState.update { state ->
            val scene = state.scenes.getOrNull(index) ?: return@update state
            val mutable = state.scenes.toMutableList()
            mutable[index] = scene.copy(
                backgroundResId = resId,
                backgroundImageUri = null
            )
            state.copy(scenes = mutable)
        }
    }

    fun addImageOverlayToCurrentScene(uriString: String) {
        _uiState.update { state ->
            val idx = state.selectedSceneIndex
            val scene = state.scenes.getOrNull(idx) ?: return@update state
            val overlay = OverlayElement(
                type = OverlayType.IMAGE_STICKER,
                text = "Logo",
                imageUri = uriString,
                xFraction = 0.78f,
                yFraction = 0.24f,
                scale = 1.0f
            )
            val mutable = state.scenes.toMutableList()
            mutable[idx] = scene.copy(overlays = scene.overlays + overlay)
            state.copy(
                scenes = mutable,
                selectedOverlayId = overlay.id,
                isPlaying = false,
                statusBanner = "ছবি/লোগো যোগ হয়েছে — প্রিভিউতে ড্র্যাগ করে বসান"
            )
        }
    }

    fun showAddTextOverlayDialog(show: Boolean) {
        _uiState.update { it.copy(showAddTextOverlaySheet = show, isPlaying = false) }
    }

    fun addTextOverlayToCurrentScene(text: String, type: OverlayType, colorHex: Long) {
        if (text.isBlank()) return
        _uiState.update { state ->
            val idx = state.selectedSceneIndex
            val scene = state.scenes.getOrNull(idx) ?: return@update state
            val overlay = OverlayElement(
                type = type,
                text = text.trim(),
                xFraction = 0.5f,
                yFraction = 0.26f,
                scale = 1.0f,
                colorHex = colorHex
            )
            val mutable = state.scenes.toMutableList()
            mutable[idx] = scene.copy(overlays = scene.overlays + overlay)
            state.copy(
                scenes = mutable,
                selectedOverlayId = overlay.id,
                showAddTextOverlaySheet = false,
                statusBanner = "টেক্সট এলিমেন্ট যোগ হয়েছে — প্রিভিউতে ড্র্যাগ করুন"
            )
        }
    }

    fun selectOverlay(overlayId: String?) {
        _uiState.update { it.copy(selectedOverlayId = overlayId) }
    }

    fun moveOverlay(sceneIndex: Int, overlayId: String, newXFraction: Float, newYFraction: Float) {
        _uiState.update { state ->
            val scene = state.scenes.getOrNull(sceneIndex) ?: return@update state
            val updatedOverlays = scene.overlays.map { ov ->
                if (ov.id == overlayId) {
                    ov.copy(
                        xFraction = newXFraction.coerceIn(0.08f, 0.92f),
                        yFraction = newYFraction.coerceIn(0.10f, 0.86f)
                    )
                } else ov
            }
            val mutable = state.scenes.toMutableList()
            mutable[sceneIndex] = scene.copy(overlays = updatedOverlays)
            state.copy(scenes = mutable)
        }
    }

    fun updateOverlayScale(sceneIndex: Int, overlayId: String, scale: Float) {
        _uiState.update { state ->
            val scene = state.scenes.getOrNull(sceneIndex) ?: return@update state
            val updatedOverlays = scene.overlays.map { ov ->
                if (ov.id == overlayId) ov.copy(scale = scale.coerceIn(0.5f, 2.2f)) else ov
            }
            val mutable = state.scenes.toMutableList()
            mutable[sceneIndex] = scene.copy(overlays = updatedOverlays)
            state.copy(scenes = mutable)
        }
    }

    fun deleteOverlay(sceneIndex: Int, overlayId: String) {
        _uiState.update { state ->
            val scene = state.scenes.getOrNull(sceneIndex) ?: return@update state
            val mutable = state.scenes.toMutableList()
            mutable[sceneIndex] = scene.copy(overlays = scene.overlays.filterNot { it.id == overlayId })
            state.copy(
                scenes = mutable,
                selectedOverlayId = if (state.selectedOverlayId == overlayId) null else state.selectedOverlayId
            )
        }
    }

    fun setExportResolution(preset: ResolutionPreset) {
        _uiState.update { it.copy(selectedResolution = preset) }
    }

    fun setExportFps(fps: Int) {
        _uiState.update { it.copy(selectedFps = fps) }
    }

    fun openExportSheet(immediateDirectExport: Boolean = false) {
        _uiState.update {
            it.copy(
                isPlaying = false,
                showExportSheet = true,
                exportState = if (immediateDirectExport) it.exportState else ExportState.Idle
            )
        }
        if (immediateDirectExport) {
            startExport()
        }
    }

    fun closeExportSheet() {
        _uiState.update { it.copy(showExportSheet = false) }
    }

    fun startExport() {
        val state = _uiState.value
        val res = state.selectedResolution
        val fps = state.selectedFps
        exportJob?.cancel()
        exportJob = viewModelScope.launch {
            _uiState.update {
                it.copy(exportState = ExportState.Exporting(0f, 0, (state.totalDurationSec * fps).toInt().coerceAtLeast(1), res))
            }
            try {
                val result = videoExporter.exportMp4(
                    scenes = state.scenes,
                    channelName = state.channelName,
                    resolution = res,
                    fps = fps
                ) { progress, frame, total ->
                    _uiState.update {
                        it.copy(exportState = ExportState.Exporting(progress, frame, total, res))
                    }
                }
                _uiState.update {
                    it.copy(exportState = ExportState.Completed(result))
                }
            } catch (_: CancellationException) {
                _uiState.update { it.copy(exportState = ExportState.Idle) }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(exportState = ExportState.Error(e.message ?: "Video encoding failed"))
                }
            }
        }
    }

    fun cancelExport() {
        exportJob?.cancel()
        _uiState.update { it.copy(exportState = ExportState.Idle) }
    }

    fun dismissStatusBanner() {
        _uiState.update { it.copy(statusBanner = null) }
    }

    override fun onCleared() {
        playbackJob?.cancel()
        exportJob?.cancel()
        super.onCleared()
    }
}
