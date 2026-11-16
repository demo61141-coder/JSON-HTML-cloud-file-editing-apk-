package com.example.export

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaCodecList
import android.media.MediaFormat
import android.media.MediaMuxer
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.example.model.DocumentaryScene
import com.example.model.ResolutionPreset
import com.example.render.SceneFrameRenderer
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.nio.ByteBuffer
import kotlin.coroutines.coroutineContext
import kotlin.math.roundToInt

data class ExportResult(
    val cacheFile: File,
    val mediaStoreUri: Uri?,
    val resolution: ResolutionPreset,
    val durationSec: Float,
    val fileSizeBytes: Long
)

class VideoExporter(private val context: Context) {

    suspend fun exportMp4(
        scenes: List<DocumentaryScene>,
        channelName: String,
        resolution: ResolutionPreset,
        fps: Int = 24,
        onProgress: (progress: Float, currentFrame: Int, totalFrames: Int) -> Unit
    ): ExportResult = withContext(Dispatchers.Default) {
        if (scenes.isEmpty()) {
            throw IllegalStateException("No scenes available to export.")
        }

        val exportDir = File(context.cacheDir, "exports").apply { mkdirs() }
        val fileName = "newscut_${resolution.label}_${System.currentTimeMillis()}.mp4"
        val outFile = File(exportDir, fileName)
        if (outFile.exists()) outFile.delete()

        val totalDurationSec = scenes.sumOf { it.durationSec.toDouble() }.toFloat().coerceAtLeast(1f)
        val totalFrames = (totalDurationSec * fps).roundToInt().coerceAtLeast(1)
        val frameDurationUs = 1_000_000L / fps
        val renderer = SceneFrameRenderer(context)

        val width = resolution.width
        val height = resolution.height

        try {
            encodeViaSurface(
                outFile = outFile,
                width = width,
                height = height,
                bitrate = resolution.bitrateBps,
                fps = fps,
                totalFrames = totalFrames,
                totalDurationSec = totalDurationSec,
                frameDurationUs = frameDurationUs,
                scenes = scenes,
                channelName = channelName,
                renderer = renderer,
                onProgress = onProgress
            )
        } catch (ce: CancellationException) {
            outFile.delete()
            throw ce
        } catch (_: Exception) {
            // Fallback to ByteBuffer YUV encoding if Surface input is unsupported on this device/codec
            if (outFile.exists()) outFile.delete()
            encodeViaByteBufferFallback(
                outFile = outFile,
                width = width,
                height = height,
                bitrate = resolution.bitrateBps,
                fps = fps,
                totalFrames = totalFrames,
                totalDurationSec = totalDurationSec,
                frameDurationUs = frameDurationUs,
                scenes = scenes,
                channelName = channelName,
                renderer = renderer,
                onProgress = onProgress
            )
        }

        val savedMediaUri = saveToMediaStoreMovies(outFile, fileName)
        ExportResult(
            cacheFile = outFile,
            mediaStoreUri = savedMediaUri,
            resolution = resolution,
            durationSec = totalDurationSec,
            fileSizeBytes = outFile.length()
        )
    }

    private suspend fun encodeViaSurface(
        outFile: File,
        width: Int,
        height: Int,
        bitrate: Int,
        fps: Int,
        totalFrames: Int,
        totalDurationSec: Float,
        frameDurationUs: Long,
        scenes: List<DocumentaryScene>,
        channelName: String,
        renderer: SceneFrameRenderer,
        onProgress: (Float, Int, Int) -> Unit
    ) {
        val format = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, width, height).apply {
            setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface)
            setInteger(MediaFormat.KEY_BIT_RATE, bitrate)
            setInteger(MediaFormat.KEY_FRAME_RATE, fps)
            setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1)
        }

        val codec = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_AVC)
        var muxer: MediaMuxer? = null
        var surface: android.view.Surface? = null
        var muxerStarted = false
        var trackIndex = -1
        var outFrameIndex = 0L
        var lastPtsUs = -1L

        try {
            codec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            surface = codec.createInputSurface()
            codec.start()
            muxer = MediaMuxer(outFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)

            val bufferInfo = MediaCodec.BufferInfo()

            for (frameIdx in 0 until totalFrames) {
                coroutineContext.ensureActive()
                val globalTimeSec = frameIdx.toFloat() / fps.toFloat()
                val (sceneIndex, scene, localProgress) = resolveSceneAtTime(scenes, globalTimeSec)

                val canvas = surface.lockCanvas(null)
                try {
                    renderer.drawFrame(
                        canvas = canvas,
                        width = width,
                        height = height,
                        scene = scene,
                        sceneIndex = sceneIndex,
                        totalScenes = scenes.size,
                        sceneProgress = localProgress,
                        globalTimeSec = globalTimeSec,
                        totalDurationSec = totalDurationSec,
                        channelName = channelName
                    )
                } finally {
                    surface.unlockCanvasAndPost(canvas)
                }

                val drainResult = drainEncoder(
                    codec = codec,
                    muxer = muxer,
                    bufferInfo = bufferInfo,
                    endOfStream = false,
                    muxerStarted = muxerStarted,
                    trackIndex = trackIndex,
                    outFrameIndex = outFrameIndex,
                    frameDurationUs = frameDurationUs,
                    lastPtsUs = lastPtsUs
                )
                muxerStarted = drainResult.muxerStarted
                trackIndex = drainResult.trackIndex
                outFrameIndex = drainResult.outFrameIndex
                lastPtsUs = drainResult.lastPtsUs

                onProgress((frameIdx + 1).toFloat() / totalFrames.toFloat(), frameIdx + 1, totalFrames)
            }

            codec.signalEndOfInputStream()
            drainEncoder(
                codec = codec,
                muxer = muxer,
                bufferInfo = bufferInfo,
                endOfStream = true,
                muxerStarted = muxerStarted,
                trackIndex = trackIndex,
                outFrameIndex = outFrameIndex,
                frameDurationUs = frameDurationUs,
                lastPtsUs = lastPtsUs
            )
        } finally {
            try {
                surface?.release()
            } catch (_: Exception) { }
            try {
                codec.stop()
            } catch (_: Exception) { }
            try {
                codec.release()
            } catch (_: Exception) { }
            try {
                if (muxerStarted) muxer?.stop()
            } catch (_: Exception) { }
            try {
                muxer?.release()
            } catch (_: Exception) { }
        }
    }

    private suspend fun encodeViaByteBufferFallback(
        outFile: File,
        width: Int,
        height: Int,
        bitrate: Int,
        fps: Int,
        totalFrames: Int,
        totalDurationSec: Float,
        frameDurationUs: Long,
        scenes: List<DocumentaryScene>,
        channelName: String,
        renderer: SceneFrameRenderer,
        onProgress: (Float, Int, Int) -> Unit
    ) {
        val codecInfo = selectAvcEncoder() ?: throw IllegalStateException("No H.264 encoder available on this device.")
        val colorFormat = selectSupportedYuvColorFormat(codecInfo)
            ?: MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar

        val format = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, width, height).apply {
            setInteger(MediaFormat.KEY_COLOR_FORMAT, colorFormat)
            setInteger(MediaFormat.KEY_BIT_RATE, bitrate)
            setInteger(MediaFormat.KEY_FRAME_RATE, fps)
            setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1)
        }

        val codec = MediaCodec.createByCodecName(codecInfo.name)
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val argbPixels = IntArray(width * height)
        val yuvSize = width * height * 3 / 2
        val yuvBuffer = ByteArray(yuvSize)
        val isPlanar = colorFormat == MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Planar

        var muxer: MediaMuxer? = null
        var muxerStarted = false
        var trackIndex = -1
        var outFrameIndex = 0L
        var lastPtsUs = -1L

        try {
            codec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            codec.start()
            muxer = MediaMuxer(outFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
            val bufferInfo = MediaCodec.BufferInfo()

            for (frameIdx in 0 until totalFrames) {
                coroutineContext.ensureActive()
                val globalTimeSec = frameIdx.toFloat() / fps.toFloat()
                val (sceneIndex, scene, localProgress) = resolveSceneAtTime(scenes, globalTimeSec)

                renderer.drawFrame(
                    canvas = canvas,
                    width = width,
                    height = height,
                    scene = scene,
                    sceneIndex = sceneIndex,
                    totalScenes = scenes.size,
                    sceneProgress = localProgress,
                    globalTimeSec = globalTimeSec,
                    totalDurationSec = totalDurationSec,
                    channelName = channelName
                )

                bitmap.getPixels(argbPixels, 0, width, 0, 0, width, height)
                encodeArgbToYuv420(argbPixels, yuvBuffer, width, height, isPlanar)

                val inIdx = codec.dequeueInputBuffer(10_000)
                if (inIdx >= 0) {
                    val inputBuffer: ByteBuffer? = codec.getInputBuffer(inIdx)
                    inputBuffer?.clear()
                    inputBuffer?.put(yuvBuffer, 0, yuvSize)
                    codec.queueInputBuffer(inIdx, 0, yuvSize, frameIdx * frameDurationUs, 0)
                }

                val drainResult = drainEncoder(
                    codec = codec,
                    muxer = muxer,
                    bufferInfo = bufferInfo,
                    endOfStream = false,
                    muxerStarted = muxerStarted,
                    trackIndex = trackIndex,
                    outFrameIndex = outFrameIndex,
                    frameDurationUs = frameDurationUs,
                    lastPtsUs = lastPtsUs
                )
                muxerStarted = drainResult.muxerStarted
                trackIndex = drainResult.trackIndex
                outFrameIndex = drainResult.outFrameIndex
                lastPtsUs = drainResult.lastPtsUs

                onProgress((frameIdx + 1).toFloat() / totalFrames.toFloat(), frameIdx + 1, totalFrames)
            }

            val eosIdx = codec.dequeueInputBuffer(10_000)
            if (eosIdx >= 0) {
                codec.queueInputBuffer(eosIdx, 0, 0, totalFrames * frameDurationUs, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
            }
            drainEncoder(
                codec = codec,
                muxer = muxer,
                bufferInfo = bufferInfo,
                endOfStream = true,
                muxerStarted = muxerStarted,
                trackIndex = trackIndex,
                outFrameIndex = outFrameIndex,
                frameDurationUs = frameDurationUs,
                lastPtsUs = lastPtsUs
            )
        } finally {
            bitmap.recycle()
            try { codec.stop() } catch (_: Exception) { }
            try { codec.release() } catch (_: Exception) { }
            try { if (muxerStarted) muxer?.stop() } catch (_: Exception) { }
            try { muxer?.release() } catch (_: Exception) { }
        }
    }

    private data class DrainState(
        val muxerStarted: Boolean,
        val trackIndex: Int,
        val outFrameIndex: Long,
        val lastPtsUs: Long
    )

    private fun drainEncoder(
        codec: MediaCodec,
        muxer: MediaMuxer,
        bufferInfo: MediaCodec.BufferInfo,
        endOfStream: Boolean,
        muxerStarted: Boolean,
        trackIndex: Int,
        outFrameIndex: Long,
        frameDurationUs: Long,
        lastPtsUs: Long
    ): DrainState {
        var started = muxerStarted
        var tIndex = trackIndex
        var frameCount = outFrameIndex
        var lastPts = lastPtsUs

        while (true) {
            val status = codec.dequeueOutputBuffer(bufferInfo, if (endOfStream) 10_000L else 0L)
            if (status == MediaCodec.INFO_TRY_AGAIN_LATER) {
                if (!endOfStream) break
            } else if (status == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                if (!started) {
                    tIndex = muxer.addTrack(codec.outputFormat)
                    muxer.start()
                    started = true
                }
            } else if (status >= 0) {
                val encodedData = codec.getOutputBuffer(status)
                if (encodedData != null) {
                    if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
                        bufferInfo.size = 0
                    }
                    if (bufferInfo.size > 0 && started) {
                        encodedData.position(bufferInfo.offset)
                        encodedData.limit(bufferInfo.offset + bufferInfo.size)
                        val pts = maxOf(lastPts + 1L, frameCount * frameDurationUs)
                        bufferInfo.presentationTimeUs = pts
                        lastPts = pts
                        frameCount++
                        muxer.writeSampleData(tIndex, encodedData, bufferInfo)
                    }
                }
                codec.releaseOutputBuffer(status, false)
                if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                    break
                }
            }
        }
        return DrainState(started, tIndex, frameCount, lastPts)
    }

    private fun selectAvcEncoder(): MediaCodecInfo? {
        val list = MediaCodecList(MediaCodecList.REGULAR_CODECS)
        return list.codecInfos.firstOrNull { info ->
            info.isEncoder && info.supportedTypes.any { it.equals(MediaFormat.MIMETYPE_VIDEO_AVC, ignoreCase = true) }
        }
    }

    private fun selectSupportedYuvColorFormat(codecInfo: MediaCodecInfo): Int? {
        val caps = codecInfo.getCapabilitiesForType(MediaFormat.MIMETYPE_VIDEO_AVC)
        val preferred = listOf(
            MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar,
            MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Planar,
            MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Flexible
        )
        return preferred.firstOrNull { it in caps.colorFormats }
    }

    private fun encodeArgbToYuv420(argb: IntArray, yuv: ByteArray, width: Int, height: Int, planar: Boolean) {
        val frameSize = width * height
        var yIndex = 0
        var uvIndex = frameSize
        val uOffset = frameSize
        val vOffset = frameSize + frameSize / 4
        var uIndex = uOffset
        var vIndex = vOffset

        for (j in 0 until height) {
            for (i in 0 until width) {
                val pixel = argb[j * width + i]
                val r = (pixel shr 16) and 0xFF
                val g = (pixel shr 8) and 0xFF
                val b = pixel and 0xFF

                val y = ((66 * r + 129 * g + 25 * b + 128) shr 8) + 16
                yuv[yIndex++] = y.coerceIn(0, 255).toByte()

                if ((j and 1) == 0 && (i and 1) == 0) {
                    val u = ((-38 * r - 74 * g + 112 * b + 128) shr 8) + 128
                    val v = ((112 * r - 94 * g - 18 * b + 128) shr 8) + 128
                    if (planar) {
                        yuv[uIndex++] = u.coerceIn(0, 255).toByte()
                        yuv[vIndex++] = v.coerceIn(0, 255).toByte()
                    } else {
                        yuv[uvIndex++] = u.coerceIn(0, 255).toByte()
                        yuv[uvIndex++] = v.coerceIn(0, 255).toByte()
                    }
                }
            }
        }
    }

    private fun resolveSceneAtTime(
        scenes: List<DocumentaryScene>,
        globalTimeSec: Float
    ): Triple<Int, DocumentaryScene, Float> {
        var acc = 0f
        for (i in scenes.indices) {
            val s = scenes[i]
            val end = acc + s.durationSec
            if (globalTimeSec <= end || i == scenes.lastIndex) {
                val progress = ((globalTimeSec - acc) / s.durationSec.coerceAtLeast(0.1f)).coerceIn(0f, 1f)
                return Triple(i, s, progress)
            }
            acc = end
        }
        return Triple(0, scenes.first(), 0f)
    }

    private fun saveToMediaStoreMovies(sourceFile: File, displayName: String): Uri? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return null
        return try {
            val values = ContentValues().apply {
                put(MediaStore.Video.Media.DISPLAY_NAME, displayName)
                put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
                put(MediaStore.Video.Media.RELATIVE_PATH, Environment.DIRECTORY_MOVIES + "/NewsCutStudio")
                put(MediaStore.Video.Media.IS_PENDING, 1)
            }
            val resolver = context.contentResolver
            val uri = resolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values) ?: return null
            resolver.openOutputStream(uri)?.use { out ->
                FileInputStream(sourceFile).use { input ->
                    input.copyTo(out)
                }
            }
            values.clear()
            values.put(MediaStore.Video.Media.IS_PENDING, 0)
            resolver.update(uri, values, null, null)
            uri
        } catch (_: Exception) {
            null
        }
    }
}
