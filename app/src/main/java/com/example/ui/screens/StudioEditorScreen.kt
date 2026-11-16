package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AddPhotoAlternate
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material.icons.rounded.TextFields
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.example.render.SceneFrameRenderer
import com.example.ui.theme.BroadcastCrimson
import com.example.ui.theme.InvestigativeAmber
import com.example.ui.theme.SignalCyan
import com.example.ui.theme.StudioBorder
import com.example.ui.theme.StudioCard
import com.example.ui.theme.StudioCardElevated
import com.example.ui.theme.StudioObsidian
import com.example.ui.theme.StudioSlate
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.viewmodel.StudioUiState
import kotlin.math.abs

@Composable
fun StudioEditorScreen(
    uiState: StudioUiState,
    onBackToImport: () -> Unit,
    onTogglePlayPause: () -> Unit,
    onToggleLoop: () -> Unit,
    onSeekTo: (Float) -> Unit,
    onStepScene: (Int) -> Unit,
    onSelectScene: (Int) -> Unit,
    onOpenSceneEdit: (Int) -> Unit,
    onAddScene: () -> Unit,
    onDuplicateScene: (Int) -> Unit,
    onDeleteScene: (Int) -> Unit,
    onMoveScene: (Int, Int) -> Unit,
    onAddImageOverlay: (String) -> Unit,
    onSetSceneBgPhoto: (Int, String?) -> Unit,
    onOpenAddTextOverlay: () -> Unit,
    onSelectOverlay: (String?) -> Unit,
    onMoveOverlay: (Int, String, Float, Float) -> Unit,
    onDeleteOverlay: (Int, String) -> Unit,
    onUpdateOverlayScale: (Int, String, Float) -> Unit,
    onOpenExportSheet: (Boolean) -> Unit
) {
    BackHandler { onBackToImport() }

    val context = LocalContext.current
    val renderer = remember(context) { SceneFrameRenderer(context) }
    var canvasSize by remember { mutableStateOf(IntSize(1, 1)) }

    val overlayPhotoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            onAddImageOverlay(uri.toString())
        }
    }

    val bgPhotoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            onSetSceneBgPhoto(uiState.selectedSceneIndex, uri.toString())
        }
    }

    val (activeSceneIdx, activeScene, sceneProgress) = uiState.resolvePlaybackAt(uiState.currentTimeSec)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(StudioObsidian)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        // Top Compact Studio Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(StudioSlate)
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                IconButton(
                    onClick = onBackToImport,
                    modifier = Modifier.testTag("back_to_import_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = "Back to Import Dashboard",
                        tint = TextPrimary
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = uiState.projectTitle,
                        style = MaterialTheme.typography.titleSmall,
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${uiState.scenes.size} Scenes • ${uiState.selectedResolution.label} 9:16",
                        style = MaterialTheme.typography.labelMedium,
                        color = InvestigativeAmber
                    )
                }
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { onOpenExportSheet(false) },
                    modifier = Modifier.testTag("open_export_sheet_button")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Tune,
                        contentDescription = "Custom Export Settings",
                        tint = TextPrimary
                    )
                }

                Button(
                    onClick = { onOpenExportSheet(true) },
                    modifier = Modifier
                        .height(40.dp)
                        .testTag("direct_export_button"),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = BroadcastCrimson)
                ) {
                    Icon(Icons.Rounded.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(5.dp))
                    Text("Export", style = MaterialTheme.typography.labelLarge)
                }
            }
        }

        // Center 9:16 Video Preview Area (adaptive weight)
        BoxWithConstraints(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            val previewModifier = Modifier
                .fillMaxHeight()
                .aspectRatio(9f / 16f, matchHeightConstraintsFirst = true)
                .clip(RoundedCornerShape(18.dp))
                .border(1.5.dp, StudioBorder, RoundedCornerShape(18.dp))
                .background(Color.Black)
                .onSizeChanged { canvasSize = it }
                .pointerInput(activeSceneIdx, activeScene.overlays, canvasSize) {
                    detectTapGestures { tapOffset ->
                        val cw = canvasSize.width.toFloat().coerceAtLeast(1f)
                        val ch = canvasSize.height.toFloat().coerceAtLeast(1f)
                        val hit = activeScene.overlays.lastOrNull { ov ->
                            val ox = ov.xFraction * cw
                            val oy = ov.yFraction * ch
                            abs(tapOffset.x - ox) < 64f && abs(tapOffset.y - oy) < 48f
                        }
                        if (hit != null) {
                            onSelectOverlay(hit.id)
                        } else {
                            onSelectOverlay(null)
                            onTogglePlayPause()
                        }
                    }
                }
                .pointerInput(activeSceneIdx, activeScene.overlays, canvasSize) {
                    var draggedOverlayId: String? = null
                    detectDragGestures(
                        onDragStart = { startOffset ->
                            val cw = canvasSize.width.toFloat().coerceAtLeast(1f)
                            val ch = canvasSize.height.toFloat().coerceAtLeast(1f)
                            draggedOverlayId = activeScene.overlays.lastOrNull { ov ->
                                val ox = ov.xFraction * cw
                                val oy = ov.yFraction * ch
                                abs(startOffset.x - ox) < 88f && abs(startOffset.y - oy) < 68f
                            }?.id
                            if (draggedOverlayId != null) {
                                onSelectOverlay(draggedOverlayId)
                            }
                        },
                        onDrag = { change, dragAmount ->
                            val id = draggedOverlayId ?: return@detectDragGestures
                            change.consume()
                            val cw = canvasSize.width.toFloat().coerceAtLeast(1f)
                            val ch = canvasSize.height.toFloat().coerceAtLeast(1f)
                            val ov = activeScene.overlays.firstOrNull { it.id == id } ?: return@detectDragGestures
                            val nx = ov.xFraction + (dragAmount.x / cw)
                            val ny = ov.yFraction + (dragAmount.y / ch)
                            onMoveOverlay(activeSceneIdx, id, nx, ny)
                        },
                        onDragEnd = { draggedOverlayId = null }
                    )
                }

            Box(modifier = previewModifier) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawIntoCanvas { canvas ->
                        renderer.drawFrame(
                            canvas = canvas.nativeCanvas,
                            width = size.width.toInt(),
                            height = size.height.toInt(),
                            scene = activeScene,
                            sceneIndex = activeSceneIdx,
                            totalScenes = uiState.scenes.size,
                            sceneProgress = sceneProgress,
                            globalTimeSec = uiState.currentTimeSec,
                            totalDurationSec = uiState.totalDurationSec,
                            channelName = uiState.channelName
                        )
                    }

                    // Draw selection highlight around active draggable overlay
                    val selectedOv = activeScene.overlays.firstOrNull { it.id == uiState.selectedOverlayId }
                    if (selectedOv != null) {
                        val cx = selectedOv.xFraction * size.width
                        val cy = selectedOv.yFraction * size.height
                        drawCircle(
                            color = SignalCyan,
                            radius = 28f,
                            center = Offset(cx, cy),
                            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 3f)
                        )
                    }
                }
            }
        }

        // Selected Overlay Quick Inspector (if an overlay element is selected)
        val selectedOv = activeScene.overlays.firstOrNull { it.id == uiState.selectedOverlayId }
        if (selectedOv != null) {
            Surface(
                color = StudioCardElevated,
                border = BorderStroke(1.dp, SignalCyan.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 2.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "ড্র্যাগ করে সরান • সাইজ:",
                        style = MaterialTheme.typography.labelSmall,
                        color = SignalCyan
                    )
                    Slider(
                        value = selectedOv.scale,
                        onValueChange = { onUpdateOverlayScale(activeSceneIdx, selectedOv.id, it) },
                        valueRange = 0.5f..2.0f,
                        modifier = Modifier.weight(1f),
                        colors = SliderDefaults.colors(thumbColor = SignalCyan, activeTrackColor = SignalCyan)
                    )
                    IconButton(
                        onClick = { onDeleteOverlay(activeSceneIdx, selectedOv.id) },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(Icons.Rounded.DeleteOutline, contentDescription = "Delete Overlay", tint = BroadcastCrimson)
                    }
                }
            }
        }

        // Quick Add Elements & Scene Edit Strip
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            contentPadding = PaddingValues(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                StudioToolChip(
                    icon = Icons.Rounded.Edit,
                    label = "এডিট সিন",
                    accent = InvestigativeAmber,
                    testTag = "edit_scene_chip",
                    onClick = { onOpenSceneEdit(activeSceneIdx) }
                )
            }
            item {
                StudioToolChip(
                    icon = Icons.Rounded.AddPhotoAlternate,
                    label = "+ লোগো/ছবি",
                    accent = SignalCyan,
                    testTag = "add_logo_overlay_chip",
                    onClick = {
                        overlayPhotoPicker.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    }
                )
            }
            item {
                StudioToolChip(
                    icon = Icons.Rounded.Image,
                    label = "ব্যাকগ্রাউন্ড ছবি",
                    accent = Color(0xFF38BDF8),
                    testTag = "set_bg_photo_chip",
                    onClick = {
                        bgPhotoPicker.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    }
                )
            }
            item {
                StudioToolChip(
                    icon = Icons.Rounded.TextFields,
                    label = "+ টেক্সট ট্যাগ",
                    accent = Color(0xFFA78BFA),
                    testTag = "add_text_overlay_chip",
                    onClick = onOpenAddTextOverlay
                )
            }
        }

        // Transport & Scrubber Panel + Multi-Track Scene Timeline
        Surface(
            color = StudioSlate,
            tonalElevation = 4.dp,
            shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
            border = BorderStroke(1.dp, StudioBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Timeline Seek Slider + Timecode
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = formatTimecode(uiState.currentTimeSec),
                        style = MaterialTheme.typography.labelMedium,
                        color = InvestigativeAmber
                    )
                    Slider(
                        value = uiState.currentTimeSec.coerceIn(0f, uiState.totalDurationSec),
                        onValueChange = onSeekTo,
                        valueRange = 0f..uiState.totalDurationSec,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("timeline_scrubber"),
                        colors = SliderDefaults.colors(
                            thumbColor = InvestigativeAmber,
                            activeTrackColor = BroadcastCrimson,
                            inactiveTrackColor = StudioBorder
                        )
                    )
                    Text(
                        text = formatTimecode(uiState.totalDurationSec),
                        style = MaterialTheme.typography.labelMedium,
                        color = TextSecondary
                    )
                }

                // Playback Transport Controls
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = { onStepScene(-1) },
                            modifier = Modifier.testTag("prev_scene_button")
                        ) {
                            Icon(Icons.Rounded.SkipPrevious, contentDescription = "Previous Scene", tint = TextPrimary)
                        }

                        Surface(
                            shape = CircleShape,
                            color = BroadcastCrimson,
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .clickable(onClick = onTogglePlayPause)
                                .testTag("play_pause_button")
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = if (uiState.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                                    contentDescription = if (uiState.isPlaying) "Pause" else "Play",
                                    tint = Color.White,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }

                        IconButton(
                            onClick = { onStepScene(1) },
                            modifier = Modifier.testTag("next_scene_button")
                        ) {
                            Icon(Icons.Rounded.SkipNext, contentDescription = "Next Scene", tint = TextPrimary)
                        }

                        IconButton(
                            onClick = onToggleLoop,
                            modifier = Modifier.testTag("loop_toggle_button")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Repeat,
                                contentDescription = "Toggle Loop",
                                tint = if (uiState.isLooping) SignalCyan else TextMuted
                            )
                        }
                    }

                    // Scene Move / Duplicate / Delete Actions for active scene
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = { onMoveScene(activeSceneIdx, -1) },
                            enabled = activeSceneIdx > 0
                        ) {
                            Icon(
                                Icons.AutoMirrored.Rounded.KeyboardArrowLeft,
                                contentDescription = "Move Scene Left",
                                tint = if (activeSceneIdx > 0) TextPrimary else TextMuted
                            )
                        }
                        IconButton(
                            onClick = { onMoveScene(activeSceneIdx, 1) },
                            enabled = activeSceneIdx < uiState.scenes.lastIndex
                        ) {
                            Icon(
                                Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                                contentDescription = "Move Scene Right",
                                tint = if (activeSceneIdx < uiState.scenes.lastIndex) TextPrimary else TextMuted
                            )
                        }
                        IconButton(onClick = { onDuplicateScene(activeSceneIdx) }) {
                            Icon(Icons.Rounded.ContentCopy, contentDescription = "Duplicate Scene", tint = TextSecondary, modifier = Modifier.size(19.dp))
                        }
                        IconButton(
                            onClick = { onDeleteScene(activeSceneIdx) },
                            modifier = Modifier.testTag("delete_scene_button")
                        ) {
                            Icon(Icons.Rounded.DeleteOutline, contentDescription = "Delete Scene", tint = BroadcastCrimson, modifier = Modifier.size(21.dp))
                        }
                    }
                }

                // Multi-Track Scene Blocks Row
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 4.dp)
                ) {
                    itemsIndexed(uiState.scenes, key = { _, s -> s.id }) { idx, scene ->
                        val isSelected = idx == activeSceneIdx
                        val accent = Color(scene.palette.accentHex)
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) StudioCardElevated else StudioCard,
                            border = BorderStroke(
                                width = if (isSelected) 2.dp else 1.dp,
                                color = if (isSelected) InvestigativeAmber else StudioBorder
                            ),
                            modifier = Modifier
                                .width(136.dp)
                                .height(64.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { onSelectScene(idx) }
                                .testTag("timeline_scene_$idx")
                        ) {
                            Row(modifier = Modifier.fillMaxSize()) {
                                Box(
                                    modifier = Modifier
                                        .width(5.dp)
                                        .fillMaxHeight()
                                        .background(accent)
                                )
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(horizontal = 8.dp, vertical = 6.dp),
                                    verticalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "S${idx + 1}",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = if (isSelected) InvestigativeAmber else accent,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "${String.format("%.1f", scene.durationSec)}s",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = TextMuted
                                        )
                                    }
                                    Text(
                                        text = scene.headline,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = TextPrimary,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }

                    item {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = StudioCard.copy(alpha = 0.6f),
                            border = BorderStroke(1.dp, SignalCyan.copy(alpha = 0.6f)),
                            modifier = Modifier
                                .width(92.dp)
                                .height(64.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .clickable(onClick = onAddScene)
                                .testTag("add_scene_button")
                        ) {
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.Center,
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(Icons.Rounded.Add, contentDescription = "Add Scene", tint = SignalCyan)
                                Text("নতুন সিন", style = MaterialTheme.typography.labelSmall, color = SignalCyan)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StudioToolChip(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    accent: Color,
    testTag: String,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = StudioCard,
        border = BorderStroke(1.dp, accent.copy(alpha = 0.45f)),
        modifier = Modifier
            .height(38.dp)
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .testTag(testTag)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Icon(icon, contentDescription = label, tint = accent, modifier = Modifier.size(16.dp))
            Text(label, style = MaterialTheme.typography.labelSmall, color = TextPrimary)
        }
    }
}

private fun formatTimecode(sec: Float): String {
    val totalTenths = (sec.coerceAtLeast(0f) * 10f).toInt()
    val mins = totalTenths / 600
    val secs = (totalTenths % 600) / 10
    val tenths = totalTenths % 10
    return String.format("%02d:%02d.%d", mins, secs, tenths)
}
