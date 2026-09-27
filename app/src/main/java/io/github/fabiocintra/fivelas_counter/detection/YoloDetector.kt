package com.example.fivelas_counter

import android.content.Context
import android.graphics.Bitmap
import com.google.ai.edge.litert.Accelerator
import com.google.ai.edge.litert.CompiledModel
import io.github.fabiocintra.fivelas_counter.detection.Detection
import io.github.fabiocintra.fivelas_counter.detection.LetterboxResult

class YoloDetector(
    context: Context
) {

    companion object {
        const val INPUT_SIZE = 640
    }


    private val model = CompiledModel.create(
        context.assets,
        "best-2.tflite",
        CompiledModel.Options(Accelerator.CPU)
    )

    private val inputBuffers = model.createInputBuffers()

    private val outputBuffers = model.createOutputBuffers()

    private fun iou(
        a: Detection,
        b: Detection
    ): Float {

        val x1 = maxOf(a.x1, b.x1)
        val y1 = maxOf(a.y1, b.y1)

        val x2 = minOf(a.x2, b.x2)
        val y2 = minOf(a.y2, b.y2)

        val intersectionWidth =
            maxOf(0f, x2 - x1)

        val intersectionHeight =
            maxOf(0f, y2 - y1)

        val intersection =
            intersectionWidth * intersectionHeight

        val areaA =
            (a.x2 - a.x1) *
                    (a.y2 - a.y1)

        val areaB =
            (b.x2 - b.x1) *
                    (b.y2 - b.y1)

        val union =
            areaA + areaB - intersection

        return if (union > 0f) {
            intersection / union
        } else {
            0f
        }
    }

    private fun nms(
        detections: List<Detection>,
        iouThreshold: Float = 0.45f
    ): List<Detection> {

        val sorted =
            detections.sortedByDescending {
                it.confidence
            }.toMutableList()

        val result =
            mutableListOf<Detection>()

        while (sorted.isNotEmpty()) {

            val best = sorted.removeAt(0)

            result.add(best)

            sorted.removeAll {
                iou(best, it) > iouThreshold
            }
        }

        return result
    }

    fun detect(bitmap: Bitmap): List<Detection> {

        val startTime =
            System.currentTimeMillis()

        // 1. Mantém a proporção da imagem
        val letterboxResult =
            letterbox(bitmap)

        // 2. Converte a imagem 640x640 criada pelo letterbox
        // para o formato esperado pelo YOLO
        val input =
            bitmapToNchw(
                letterboxResult.bitmap
            )

        inputBuffers[0].writeFloat(input)

        model.run(
            inputBuffers,
            outputBuffers
        )

        val output =
            outputBuffers[0].readFloat()


        val detections = mutableListOf<Detection>()

        val numCandidates = 8400
        val confidenceThreshold = 0.70f

        for (i in 0 until numCandidates) {

            val x = output[i]
            val y = output[numCandidates + i]
            val w = output[(2 * numCandidates) + i]
            val h = output[(3 * numCandidates) + i]

            val confidence =
                output[(4 * numCandidates) + i]

            if (confidence < confidenceThreshold) {
                continue
            }

            // Coordenadas normalizadas retornadas pelo YOLO
            val x1 = x - w / 2f
            val y1 = y - h / 2f
            val x2 = x + w / 2f
            val y2 = y + h / 2f

            // =====================================
            // 1. Volta de 0..1 para 640x640
            // =====================================

            val modelX1 = x1 * INPUT_SIZE
            val modelY1 = y1 * INPUT_SIZE

            val modelX2 = x2 * INPUT_SIZE
            val modelY2 = y2 * INPUT_SIZE

            // =====================================
            // 2. Remove o padding do letterbox
            // =====================================

            val originalX1 =
                (modelX1 - letterboxResult.padX) /
                        letterboxResult.scale

            val originalY1 =
                (modelY1 - letterboxResult.padY) /
                        letterboxResult.scale

            val originalX2 =
                (modelX2 - letterboxResult.padX) /
                        letterboxResult.scale

            val originalY2 =
                (modelY2 - letterboxResult.padY) /
                        letterboxResult.scale

            // =====================================
            // 3. Normaliza para a imagem original
            // =====================================

            val normalizedX1 =
                (originalX1 / letterboxResult.originalWidth)
                    .coerceIn(0f, 1f)

            val normalizedY1 =
                (originalY1 / letterboxResult.originalHeight)
                    .coerceIn(0f, 1f)

            val normalizedX2 =
                (originalX2 / letterboxResult.originalWidth)
                    .coerceIn(0f, 1f)

            val normalizedY2 =
                (originalY2 / letterboxResult.originalHeight)
                    .coerceIn(0f, 1f)

            // =====================================
            // 4. Guarda a caixa já corrigida
            // =====================================

            detections.add(
                Detection(
                    x1 = normalizedX1,
                    y1 = normalizedY1,
                    x2 = normalizedX2,
                    y2 = normalizedY2,
                    confidence = confidence
                )
            )
        }

        val elapsed =
            System.currentTimeMillis() -
                    startTime

        android.util.Log.d(
            "YOLO",
            "Tempo: ${elapsed}ms | FPS: ${1000f / elapsed}"
        )

        return nms(detections)

    }

    private fun letterbox(
        bitmap: Bitmap,
        targetSize: Int = 640
    ): LetterboxResult {

        val originalWidth = bitmap.width
        val originalHeight = bitmap.height

        val scale = minOf(
            targetSize.toFloat() / originalWidth,
            targetSize.toFloat() / originalHeight
        )

        val newWidth =
            (originalWidth * scale).toInt()

        val newHeight =
            (originalHeight * scale).toInt()

        val resized =
            Bitmap.createScaledBitmap(
                bitmap,
                newWidth,
                newHeight,
                true
            )

        val result =
            Bitmap.createBitmap(
                targetSize,
                targetSize,
                Bitmap.Config.ARGB_8888
            )

        val canvas =
            android.graphics.Canvas(result)

        val padX =
            (targetSize - newWidth) / 2f

        val padY =
            (targetSize - newHeight) / 2f

        canvas.drawBitmap(
            resized,
            padX,
            padY,
            null
        )

        return LetterboxResult(
            bitmap = result,
            scale = scale,
            padX = padX,
            padY = padY,
            originalWidth = originalWidth,
            originalHeight = originalHeight
        )
    }
    private fun bitmapToNchw(bitmap: Bitmap): FloatArray {

        val pixels =
            IntArray(
                INPUT_SIZE * INPUT_SIZE
            )

        bitmap.getPixels(
            pixels,
            0,
            INPUT_SIZE,
            0,
            0,
            INPUT_SIZE,
            INPUT_SIZE
        )

        val area =
            INPUT_SIZE * INPUT_SIZE

        val input =
            FloatArray(
                3 * area
            )

        for (i in pixels.indices) {

            val pixel = pixels[i]

            val r =
                (pixel shr 16) and 0xFF

            val g =
                (pixel shr 8) and 0xFF

            val b =
                pixel and 0xFF

            input[i] =
                r / 255.0f

            input[area + i] =
                g / 255.0f

            input[(2 * area) + i] =
                b / 255.0f
        }

        return input
    }
}