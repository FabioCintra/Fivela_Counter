package io.github.fabiocintra.fivelas_counter.ui

import io.github.fabiocintra.fivelas_counter.detection.Detection

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas


@Composable
fun DetectionOverlay(
    detections: List<Detection>,
    frameWidth: Int,
    frameHeight: Int,
    modifier: Modifier = Modifier
) {

    Canvas(
        modifier = modifier
    ) {

        if (frameWidth == 0 || frameHeight == 0) {
            return@Canvas
        }

        val imageAspectRatio =
            frameWidth.toFloat() /
                    frameHeight.toFloat()

        val canvasAspectRatio =
            size.width /
                    size.height


        val displayedWidth: Float
        val displayedHeight: Float

        val offsetX: Float
        val offsetY: Float


        if (canvasAspectRatio > imageAspectRatio) {

            // Sobra espaço nas laterais

            displayedHeight =
                size.height

            displayedWidth =
                displayedHeight *
                        imageAspectRatio

            offsetX =
                (size.width - displayedWidth) / 2f

            offsetY = 0f

        } else {

            // Sobra espaço em cima e embaixo
            // ESTE É O QUE ESTÁ ACONTECENDO
            // NA SUA IMAGEM.

            displayedWidth =
                size.width

            displayedHeight =
                displayedWidth /
                        imageAspectRatio

            offsetX = 0f

            offsetY =
                (size.height - displayedHeight) / 2f
        }


        detections.forEach { detection ->

            val left =
                offsetX +
                        detection.x1 * displayedWidth

            val top =
                offsetY +
                        detection.y1 * displayedHeight

            val right =
                offsetX +
                        detection.x2 * displayedWidth

            val bottom =
                offsetY +
                        detection.y2 * displayedHeight


            drawRect(
                color = Color.Red,

                topLeft = Offset(
                    left,
                    top
                ),

                size = Size(
                    right - left,
                    bottom - top
                ),

                style = Stroke(
                    width = 6f
                )
            )

            drawContext.canvas.nativeCanvas.drawText(
                "Fivela ${
                    (detection.confidence * 100).toInt()
                }%",

                left,

                (top - 10f)
                    .coerceAtLeast(40f),

                android.graphics.Paint().apply {

                    color =
                        android.graphics.Color.RED

                    textSize = 40f

                    isAntiAlias = true
                }
            )
        }
    }
}