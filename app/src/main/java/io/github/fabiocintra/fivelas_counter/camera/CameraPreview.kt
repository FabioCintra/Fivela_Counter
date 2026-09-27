package io.github.fabiocintra.fivelas_counter.camera

import android.graphics.Bitmap
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.fivelas_counter.YoloDetector
import io.github.fabiocintra.fivelas_counter.detection.Detection
import java.util.concurrent.Executors

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import io.github.fabiocintra.fivelas_counter.counting.LineCounter
import io.github.fabiocintra.fivelas_counter.tracking.SimpleTracker
import io.github.fabiocintra.fivelas_counter.tracking.TrackedObject

import io.github.fabiocintra.fivelas_counter.ui.DetectionOverlay


import androidx.compose.material3.Text
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import io.github.fabiocintra.fivelas_counter.counting.StableCounter

fun rotateBitmap(
    bitmap: Bitmap,
    degrees: Int
): Bitmap {

    if (degrees == 0) {
        return bitmap
    }

    val matrix = android.graphics.Matrix()

    matrix.postRotate(
        degrees.toFloat()
    )

    return Bitmap.createBitmap(
        bitmap,
        0,
        0,
        bitmap.width,
        bitmap.height,
        matrix,
        true
    )
}
@Composable
fun CameraPreview(
    modifier: Modifier = Modifier
) {

    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val previewView = remember {
        PreviewView(context).apply {
            scaleType = PreviewView.ScaleType.FIT_CENTER
        }
    }

    var frameWidth by remember {
        mutableIntStateOf(0)
    }

    var frameHeight by remember {
        mutableIntStateOf(0)
    }

    val detector = remember {
        YoloDetector(context)
    }

    val tracker = remember {
        SimpleTracker()
    }

    val lineCounter = remember {
        LineCounter(
            lineY = 0.60f
        )
    }

    val stableCounter = remember {
        StableCounter(
            windowSize = 10,
            minAgreement = 7
        )
    }

    var tracks by remember {
        mutableStateOf<List<TrackedObject>>(
            emptyList()
        )
    }

    var count by remember {
        mutableIntStateOf(0)
    }

    var detections by remember {
        mutableStateOf<List<Detection>>(emptyList())
    }


    // Thread separada para analisar os frames
    val cameraExecutor = remember {
        Executors.newSingleThreadExecutor()
    }

    DisposableEffect(Unit) {
        onDispose {
            cameraExecutor.shutdown()
        }
    }

    Box(
        modifier = modifier.fillMaxSize()
    ) {

        // =====================================
        // CAMADA 1 - CÂMERA
        // =====================================

        AndroidView(
            modifier = Modifier.fillMaxSize(),

            factory = {

                val cameraProviderFuture =
                    ProcessCameraProvider.getInstance(context)

                cameraProviderFuture.addListener({

                    val cameraProvider =
                        cameraProviderFuture.get()

                    val preview =
                        Preview.Builder()
                            .build()

                    preview.setSurfaceProvider(
                        previewView.surfaceProvider
                    )

                    val imageAnalysis =
                        ImageAnalysis.Builder()
                            .setBackpressureStrategy(
                                ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST
                            )
                            .build()

                    imageAnalysis.setAnalyzer(
                        cameraExecutor
                    ) { imageProxy ->

                        try {

                            val bitmap =
                                imageProxy.toBitmap()

                            val rotatedBitmap =
                                rotateBitmap(
                                    bitmap,
                                    imageProxy.imageInfo.rotationDegrees
                                )

                            val newDetections =
                                detector.detect(rotatedBitmap)

                            val newTracks =
                                tracker.update(
                                    newDetections
                                )

                            lineCounter.update(
                                newTracks
                            )

                            val newCount =
                                lineCounter.count

                            ContextCompat
                                .getMainExecutor(context)
                                .execute {

                                    detections =
                                        newDetections

                                    tracks =
                                        newTracks

                                    count =
                                        newCount

                                    frameWidth =
                                        rotatedBitmap.width

                                    frameHeight =
                                        rotatedBitmap.height
                                }

                        } catch (e: Exception) {

                            e.printStackTrace()

                        } finally {

                            imageProxy.close()
                        }
                    }

                    val cameraSelector =
                        CameraSelector.DEFAULT_BACK_CAMERA

                    try {

                        cameraProvider.unbindAll()

                        cameraProvider.bindToLifecycle(
                            lifecycleOwner,
                            cameraSelector,
                            preview,
                            imageAnalysis
                        )

                    } catch (e: Exception) {

                        e.printStackTrace()
                    }

                }, ContextCompat.getMainExecutor(context))

                previewView
            }
        )

        // =====================================
        // CAMADA 2 - CAIXAS DO YOLO
        // =====================================

        DetectionOverlay(
            tracks = tracks,
            frameWidth = frameWidth,
            frameHeight = frameHeight,
            lineY = lineCounter.lineY,
            modifier = Modifier.fillMaxSize()
        )

        Text(
            text = "Contagem: $count",
            color = Color.White,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(24.dp)
                .background(
                    Color.Black.copy(alpha = 0.6f)
                )
                .padding(12.dp)
        )

        Button(
            onClick = {
                lineCounter.reset()
                count = 0
            },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(24.dp)
        ) {
            Text("Zerar")
        }
    }
}
