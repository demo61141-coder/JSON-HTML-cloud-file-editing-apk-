package com.example

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.screens.AddTextOverlayDialog
import com.example.ui.screens.ExportBottomSheet
import com.example.ui.screens.ImportDashboardScreen
import com.example.ui.screens.SceneEditBottomSheet
import com.example.ui.screens.StudioEditorScreen
import com.example.ui.theme.NewsCutStudioTheme
import com.example.viewmodel.StudioDestination
import com.example.viewmodel.StudioViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT)
        )
        setContent {
            NewsCutStudioTheme {
                NewsCutStudioApp()
            }
        }
    }
}

@Composable
fun NewsCutStudioApp(
    viewModel: StudioViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.statusBanner) {
        val msg = uiState.statusBanner ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(msg)
        viewModel.dismissStatusBanner()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Crossfade(
            targetState = uiState.destination,
            label = "studio_destination_crossfade"
        ) { destination ->
            when (destination) {
                StudioDestination.IMPORT_DASHBOARD -> {
                    ImportDashboardScreen(
                        uiState = uiState,
                        onLoadReporterHasanDefault = viewModel::loadReporterHasanDefaultAndOpen,
                        onOpenCurrentProject = viewModel::openStudioEditor,
                        onDirectExport = { viewModel.openExportSheet(immediateDirectExport = true) },
                        onScriptChange = viewModel::updateScriptInput,
                        onProcessScript = { viewModel.importScriptAndOpen(it) },
                        onImportFileUri = viewModel::importFileFromUri
                    )
                }

                StudioDestination.STUDIO_EDITOR -> {
                    StudioEditorScreen(
                        uiState = uiState,
                        onBackToImport = viewModel::goToImportDashboard,
                        onTogglePlayPause = viewModel::togglePlayPause,
                        onToggleLoop = viewModel::toggleLoop,
                        onSeekTo = viewModel::seekTo,
                        onStepScene = viewModel::stepScene,
                        onSelectScene = viewModel::selectSceneAndSeek,
                        onOpenSceneEdit = viewModel::openSceneEditSheet,
                        onAddScene = viewModel::addNewScene,
                        onDuplicateScene = viewModel::duplicateScene,
                        onDeleteScene = viewModel::deleteScene,
                        onMoveScene = viewModel::moveScene,
                        onAddImageOverlay = viewModel::addImageOverlayToCurrentScene,
                        onSetSceneBgPhoto = viewModel::setSceneBackgroundPhoto,
                        onOpenAddTextOverlay = { viewModel.showAddTextOverlayDialog(true) },
                        onSelectOverlay = viewModel::selectOverlay,
                        onMoveOverlay = viewModel::moveOverlay,
                        onDeleteOverlay = viewModel::deleteOverlay,
                        onUpdateOverlayScale = viewModel::updateOverlayScale,
                        onOpenExportSheet = { direct -> viewModel.openExportSheet(immediateDirectExport = direct) }
                    )
                }
            }
        }

        if (uiState.showSceneEditSheet) {
            val idx = uiState.selectedSceneIndex
            val scene = uiState.scenes.getOrNull(idx)
            if (scene != null) {
                SceneEditBottomSheet(
                    sceneIndex = idx,
                    scene = scene,
                    canDeleteScene = uiState.scenes.size > 1,
                    onDismiss = viewModel::closeSceneEditSheet,
                    onUpdateScene = { updated -> viewModel.updateScene(idx, updated) },
                    onDeleteScene = { viewModel.deleteScene(idx) },
                    onPickBackgroundPhoto = { uri -> viewModel.setSceneBackgroundPhoto(idx, uri) },
                    onSelectBuiltInBackdrop = { resId -> viewModel.setSceneBuiltInBackdrop(idx, resId) },
                    onDeleteOverlay = { overlayId -> viewModel.deleteOverlay(idx, overlayId) }
                )
            }
        }

        if (uiState.showAddTextOverlaySheet) {
            AddTextOverlayDialog(
                onDismiss = { viewModel.showAddTextOverlayDialog(false) },
                onAdd = { text, type, color ->
                    viewModel.addTextOverlayToCurrentScene(text, type, color)
                }
            )
        }

        if (uiState.showExportSheet) {
            ExportBottomSheet(
                uiState = uiState,
                onDismiss = viewModel::closeExportSheet,
                onSelectResolution = viewModel::setExportResolution,
                onSelectFps = viewModel::setExportFps,
                onStartExport = viewModel::startExport,
                onCancelExport = viewModel::cancelExport
            )
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(12.dp)
        )
    }
}
