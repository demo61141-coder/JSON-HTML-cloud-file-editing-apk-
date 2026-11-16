package com.example.ui.screens

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.PlayCircle
import androidx.compose.material.icons.rounded.SaveAlt
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.example.R
import com.example.export.HtmlStudioExporter
import com.example.model.DocumentaryScene
import com.example.model.OverlayType
import com.example.model.ResolutionPreset
import com.example.model.SceneAnimationStyle
import com.example.model.SceneThemePalette
import com.example.ui.theme.BroadcastCrimson
import com.example.ui.theme.EmeraldLive
import com.example.ui.theme.InvestigativeAmber
import com.example.ui.theme.SignalCyan
import com.example.ui.theme.StudioBorder
import com.example.ui.theme.StudioCard
import com.example.ui.theme.StudioCardElevated
import com.example.ui.theme.StudioSlate
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.viewmodel.ExportState
import com.example.viewmodel.StudioUiState
import java.io.FileInputStream

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SceneEditBottomSheet(
    sceneIndex: Int,
    scene: DocumentaryScene,
    canDeleteScene: Boolean,
    onDismiss: () -> Unit,
    onUpdateScene: (DocumentaryScene) -> Unit,
    onDeleteScene: () -> Unit,
    onPickBackgroundPhoto: (String?) -> Unit,
    onSelectBuiltInBackdrop: (Int?) -> Unit,
    onDeleteOverlay: (String) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            onPickBackgroundPhoto(uri.toString())
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = StudioSlate,
        contentColor = TextPrimary
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "দৃশ্য #${sceneIndex + 1} এডিট করুন",
                    style = MaterialTheme.typography.titleLarge,
                    color = TextPrimary
                )
                if (canDeleteScene) {
                    TextButton(onClick = onDeleteScene) {
                        Icon(Icons.Rounded.DeleteOutline, contentDescription = null, tint = BroadcastCrimson)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("ডিলিট", color = BroadcastCrimson)
                    }
                }
            }

            OutlinedTextField(
                value = scene.badge,
                onValueChange = { onUpdateScene(scene.copy(badge = it)) },
                label = { Text("ব্যাজ / ক্যাটাগরি (Badge)") },
                modifier = Modifier.fillMaxWidth().testTag("edit_badge_field"),
                singleLine = true
            )

            OutlinedTextField(
                value = scene.headline,
                onValueChange = { onUpdateScene(scene.copy(headline = it)) },
                label = { Text("মূল শিরোনাম (Headline)") },
                modifier = Modifier.fillMaxWidth().testTag("edit_headline_field"),
                maxLines = 3
            )

            OutlinedTextField(
                value = scene.subtext,
                onValueChange = { onUpdateScene(scene.copy(subtext = it)) },
                label = { Text("বিস্তারিত বর্ণনা (Subtext)") },
                modifier = Modifier.fillMaxWidth().testTag("edit_subtext_field"),
                minLines = 2,
                maxLines = 4
            )

            OutlinedTextField(
                value = scene.tickerText,
                onValueChange = { onUpdateScene(scene.copy(tickerText = it)) },
                label = { Text("নিচের ব্রেকিং টিকার (Ticker)") },
                modifier = Modifier.fillMaxWidth(),
                maxLines = 2
            )

            OutlinedTextField(
                value = scene.reporterTag,
                onValueChange = { onUpdateScene(scene.copy(reporterTag = it)) },
                label = { Text("রিপোর্টার ক্রেডিট") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            // Duration Slider
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("দৃশ্যের সময়কাল (Duration)", style = MaterialTheme.typography.labelLarge, color = TextPrimary)
                    Text("${String.format("%.1f", scene.durationSec)} সেকেন্ড", style = MaterialTheme.typography.labelMedium, color = InvestigativeAmber)
                }
                Slider(
                    value = scene.durationSec,
                    onValueChange = { onUpdateScene(scene.copy(durationSec = it)) },
                    valueRange = 2.0f..15.0f,
                    colors = SliderDefaults.colors(thumbColor = InvestigativeAmber, activeTrackColor = BroadcastCrimson)
                )
            }

            // Animation Style Selector
            Text("অ্যানিমেশন স্টাইল", style = MaterialTheme.typography.labelLarge, color = TextPrimary)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                SceneAnimationStyle.entries.forEach { style ->
                    FilterChip(
                        selected = scene.animationStyle == style,
                        onClick = { onUpdateScene(scene.copy(animationStyle = style)) },
                        label = { Text(style.labelBn) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = BroadcastCrimson,
                            selectedLabelColor = Color.White
                        )
                    )
                }
            }

            // Color Palette Swatches
            Text("থিম কালার প্যালেট", style = MaterialTheme.typography.labelLarge, color = TextPrimary)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                SceneThemePalette.entries.forEach { palette ->
                    val selected = scene.palette == palette
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color(palette.accentHex))
                            .border(
                                width = if (selected) 3.dp else 1.dp,
                                color = if (selected) Color.White else StudioBorder,
                                shape = CircleShape
                            )
                            .clickable { onUpdateScene(scene.copy(palette = palette)) }
                    )
                }
            }

            // Background Image Controls
            Text("ব্যাকগ্রাউন্ড ভিজ্যুয়াল", style = MaterialTheme.typography.labelLarge, color = TextPrimary)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    }
                ) {
                    Icon(Icons.Rounded.Image, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("গ্যালারি থেকে ছবি")
                }
                OutlinedButton(
                    onClick = { onSelectBuiltInBackdrop(R.drawable.img_doc_land_office_1794844915154) }
                ) {
                    Text("ভূমি অফিস")
                }
                OutlinedButton(
                    onClick = { onSelectBuiltInBackdrop(R.drawable.img_doc_raid_dossier_1794844917233) }
                ) {
                    Text("ডসিয়ার ডেস্ক")
                }
                if (scene.backgroundImageUri != null || scene.backgroundResId != null) {
                    TextButton(onClick = { onSelectBuiltInBackdrop(null) }) {
                        Text("ছবি সরান", color = BroadcastCrimson)
                    }
                }
            }

            // Overlays List
            if (scene.overlays.isNotEmpty()) {
                Text("যুক্ত এলিমেন্টসমূহ (${scene.overlays.size})", style = MaterialTheme.typography.labelLarge, color = SignalCyan)
                scene.overlays.forEach { ov ->
                    Surface(
                        color = StudioCard,
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, StudioBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "${ov.type.label}: ${ov.text}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextPrimary,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(onClick = { onDeleteOverlay(ov.id) }) {
                                Icon(Icons.Rounded.DeleteOutline, contentDescription = "Remove overlay", tint = BroadcastCrimson)
                            }
                        }
                    }
                }
            }

            Button(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = InvestigativeAmber, contentColor = Color(0xFF1A1200))
            ) {
                Text("পরিবর্তন সেভ ও প্রিভিউ দেখুন", fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun AddTextOverlayDialog(
    onDismiss: () -> Unit,
    onAdd: (text: String, type: OverlayType, colorHex: Long) -> Unit
) {
    var text by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf(OverlayType.TEXT_BADGE) }
    var selectedColor by remember { mutableStateOf(0xFFE53935L) }
    val colorOptions = listOf(0xFFE53935L, 0xFFF59E0BL, 0xFF06B6D4L, 0xFF10B981L)

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = StudioSlate,
        titleContentColor = TextPrimary,
        textContentColor = TextPrimary,
        title = { Text("নতুন টেক্সট বা লোকেশন ট্যাগ যোগ করুন") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    label = { Text("ট্যাগের লেখা (যেমন: ডামুড্যা, এক্সক্লুসিভ)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("overlay_text_input")
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = selectedType == OverlayType.TEXT_BADGE,
                        onClick = { selectedType = OverlayType.TEXT_BADGE },
                        label = { Text("স্ট্যাম্প") }
                    )
                    FilterChip(
                        selected = selectedType == OverlayType.LOWER_THIRD,
                        onClick = { selectedType = OverlayType.LOWER_THIRD },
                        label = { Text("লোকেশন ট্যাগ") }
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    colorOptions.forEach { hex ->
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(Color(hex))
                                .border(
                                    width = if (selectedColor == hex) 3.dp else 1.dp,
                                    color = if (selectedColor == hex) Color.White else StudioBorder,
                                    shape = CircleShape
                                )
                                .clickable { selectedColor = hex }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onAdd(text, selectedType, selectedColor) },
                enabled = text.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = BroadcastCrimson)
            ) {
                Text("যুক্ত করুন")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("বাতিল", color = TextSecondary)
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExportBottomSheet(
    uiState: StudioUiState,
    onDismiss: () -> Unit,
    onSelectResolution: (ResolutionPreset) -> Unit,
    onSelectFps: (Int) -> Unit,
    onStartExport: () -> Unit,
    onCancelExport: () -> Unit
) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var dropdownExpanded by remember { mutableStateOf(false) }

    val saveMp4SafLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("video/mp4")
    ) { destUri ->
        val completed = uiState.exportState as? ExportState.Completed ?: return@rememberLauncherForActivityResult
        if (destUri != null) {
            try {
                context.contentResolver.openOutputStream(destUri)?.use { out ->
                    FileInputStream(completed.result.cacheFile).use { input ->
                        input.copyTo(out)
                    }
                }
            } catch (_: Exception) { }
        }
    }

    val saveHtmlSafLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/html")
    ) { destUri ->
        if (destUri != null) {
            try {
                val html = HtmlStudioExporter.buildStandaloneHtml(
                    projectTitle = uiState.projectTitle,
                    channelName = uiState.channelName,
                    scenes = uiState.scenes
                )
                context.contentResolver.openOutputStream(destUri)?.use { out ->
                    out.write(html.toByteArray(Charsets.UTF_8))
                }
            } catch (_: Exception) { }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = StudioSlate,
        contentColor = TextPrimary
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = "ভিডিও এক্সপোর্ট ও রেজোলিউশন (9:16)",
                style = MaterialTheme.typography.titleLarge,
                color = TextPrimary
            )

            // Resolution Dropdown (240p, 360p, 480p, 720p, 1080p)
            ExposedDropdownMenuBox(
                expanded = dropdownExpanded,
                onExpandedChange = { dropdownExpanded = !dropdownExpanded }
            ) {
                OutlinedTextField(
                    value = "${uiState.selectedResolution.label} — ${uiState.selectedResolution.dimensionsText}",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("রেজোলিউশন নির্বাচন করুন (Vertical 9:16)") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = dropdownExpanded) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor()
                        .testTag("resolution_dropdown")
                )

                ExposedDropdownMenu(
                    expanded = dropdownExpanded,
                    onDismissRequest = { dropdownExpanded = false },
                    modifier = Modifier.background(StudioCardElevated)
                ) {
                    ResolutionPreset.entries.forEach { preset ->
                        DropdownMenuItem(
                            text = {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(preset.label, fontWeight = FontWeight.Bold, color = TextPrimary)
                                    Text(preset.dimensionsText, color = InvestigativeAmber, style = MaterialTheme.typography.labelMedium)
                                }
                            },
                            onClick = {
                                onSelectResolution(preset)
                                dropdownExpanded = false
                            },
                            modifier = Modifier.testTag("resolution_option_${preset.label}")
                        )
                    }
                }
            }

            // Frame Rate Chips
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("ফ্রেম রেট:", style = MaterialTheme.typography.labelLarge, color = TextSecondary)
                listOf(24, 30).forEach { fps ->
                    FilterChip(
                        selected = uiState.selectedFps == fps,
                        onClick = { onSelectFps(fps) },
                        label = { Text("$fps FPS") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = SignalCyan.copy(alpha = 0.25f),
                            selectedLabelColor = SignalCyan
                        )
                    )
                }
            }

            when (val exportState = uiState.exportState) {
                is ExportState.Idle -> {
                    Button(
                        onClick = onStartExport,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("start_export_button"),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = BroadcastCrimson)
                    ) {
                        Icon(Icons.Rounded.Download, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("MP4 ভিডিও রেন্ডার ও সেভ করুন (${uiState.selectedResolution.label})", fontWeight = FontWeight.Bold)
                    }
                }

                is ExportState.Exporting -> {
                    Surface(
                        color = StudioCard,
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, InvestigativeAmber.copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "রেন্ডারিং ${exportState.resolution.label} (${exportState.resolution.dimensionsText})",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = InvestigativeAmber
                                )
                                Text(
                                    text = "${(exportState.progress * 100).toInt()}%",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = TextPrimary
                                )
                            }
                            LinearProgressIndicator(
                                progress = { exportState.progress },
                                modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape),
                                color = BroadcastCrimson,
                                trackColor = StudioBorder
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Frame ${exportState.currentFrame} / ${exportState.totalFrames}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = TextSecondary
                                )
                                TextButton(onClick = onCancelExport) {
                                    Text("বাতিল করুন", color = BroadcastCrimson)
                                }
                            }
                        }
                    }
                }

                is ExportState.Completed -> {
                    val res = exportState.result
                    val sizeKb = (res.fileSizeBytes / 1024L).coerceAtLeast(1L)
                    Surface(
                        color = StudioCard,
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, EmeraldLive.copy(alpha = 0.6f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = EmeraldLive)
                                Column {
                                    Text("MP4 ভিডিও সফলভাবে তৈরি হয়েছে!", style = MaterialTheme.typography.titleSmall, color = EmeraldLive)
                                    Text(
                                        text = "${res.resolution.dimensionsText} • ${String.format("%.1f", res.durationSec)}s • ${sizeKb} KB",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = TextSecondary
                                    )
                                }
                            }

                            if (res.mediaStoreUri != null) {
                                Text(
                                    text = "গ্যালারির Movies/NewsCutStudio ফোল্ডারে সেভ হয়েছে।",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextPrimary
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = {
                                        val shareUri = FileProvider.getUriForFile(
                                            context,
                                            "${context.packageName}.fileprovider",
                                            res.cacheFile
                                        )
                                        val intent = Intent(Intent.ACTION_VIEW).apply {
                                            setDataAndType(shareUri, "video/mp4")
                                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                        }
                                        context.startActivity(Intent.createChooser(intent, "Play Documentary Video"))
                                    },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldLive, contentColor = Color(0xFF042117))
                                ) {
                                    Icon(Icons.Rounded.PlayCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("প্লে করুন")
                                }

                                OutlinedButton(
                                    onClick = {
                                        val shareUri = FileProvider.getUriForFile(
                                            context,
                                            "${context.packageName}.fileprovider",
                                            res.cacheFile
                                        )
                                        val intent = Intent(Intent.ACTION_SEND).apply {
                                            type = "video/mp4"
                                            putExtra(Intent.EXTRA_STREAM, shareUri)
                                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                        }
                                        context.startActivity(Intent.createChooser(intent, "Share Video"))
                                    },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Rounded.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("শেয়ার")
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        saveMp4SafLauncher.launch("newscut_${res.resolution.label}.mp4")
                                    },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Rounded.SaveAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Save As...")
                                }

                                OutlinedButton(
                                    onClick = onStartExport,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("আবার এক্সপোর্ট")
                                }
                            }
                        }
                    }
                }

                is ExportState.Error -> {
                    Surface(
                        color = StudioCard,
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, BroadcastCrimson),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Rounded.ErrorOutline, contentDescription = null, tint = BroadcastCrimson)
                                Text(exportState.message, color = TextPrimary, style = MaterialTheme.typography.bodyMedium)
                            }
                            Button(
                                onClick = onStartExport,
                                colors = ButtonDefaults.buttonColors(containerColor = BroadcastCrimson)
                            ) {
                                Text("পুনরায় চেষ্টা করুন")
                            }
                        }
                    }
                }
            }

            // Secondary Portable Single-File HTML5 Studio Export
            OutlinedButton(
                onClick = { saveHtmlSafLauncher.launch("newscut_html5_studio.html") },
                modifier = Modifier.fillMaxWidth().height(46.dp),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, StudioBorder)
            ) {
                Icon(Icons.Rounded.Code, contentDescription = null, tint = SignalCyan, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Single-File HTML5 Studio (.html) এক্সপোর্ট করুন", color = SignalCyan)
            }

            Spacer(modifier = Modifier.height(14.dp))
        }
    }
}
