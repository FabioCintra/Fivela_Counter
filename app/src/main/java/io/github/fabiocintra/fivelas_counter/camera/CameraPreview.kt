package io.github.fabiocintra.fivelas_counter.camera

import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import java.util.concurrent.Executors

@Composable
fun CameraPreview(
    modifier: Modifier = Modifier
) {

    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val previewView = remember {
        PreviewView(context)
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

    AndroidView(
        modifier = modifier,

        factory = {

            val cameraProviderFuture =
                ProcessCameraProvider.getInstance(context)

            cameraProviderFuture.addListener({

                val cameraProvider =
                    cameraProviderFuture.get()

                // =============================
                // PREVIEW
                // =============================

                val preview =
                    Preview.Builder()
                        .build()

                preview.setSurfaceProvider(
                    previewView.surfaceProvider
                )

                // =============================
                // IMAGE ANALYSIS
                // =============================

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

                        // POR ENQUANTO SÓ TESTAMOS
                        println(
                            "Frame recebido: " +
                                    "${imageProxy.width}x${imageProxy.height}"
                        )

                        println(
                            "Rotação: " +
                                    imageProxy.imageInfo.rotationDegrees
                        )

                    } finally {

                        imageProxy.close()
                    }
                }

                // =============================
                // CÂMERA TRASEIRA
                // =============================

                val cameraSelector =
                    CameraSelector.DEFAULT_BACK_CAMERA

                try {

                    cameraProvider.unbindAll()

                    // IMPORTANTE:
                    // agora ligamos Preview + ImageAnalysis
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
}