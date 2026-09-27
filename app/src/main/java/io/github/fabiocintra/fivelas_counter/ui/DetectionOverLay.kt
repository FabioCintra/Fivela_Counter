package io.github.fabiocintra.fivelas_counter.ui

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas

import io.github.fabiocintra.fivelas_counter.tracking.TrackedObject


@Composable
fun DetectionOverlay(
    tracks: List<TrackedObject>,
    frameWidth: Int,
    frameHeight: Int,

    // posição da linha entre 0 e 1
    lineY: Float = 0.60f,

    modifier: Modifier = Modifier
) {

    Canvas(
        modifier = modifier
    ) {

        if (
            frameWidth == 0 ||
            frameHeight == 0
        ) {
            return@Canvas
        }


        // ==============================
        // TAMANHO REAL DA IMAGEM
        // DENTRO DO PREVIEW
        // ==============================

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


        if (
            canvasAspectRatio >
            imageAspectRatio
        ) {

            // sobra espaço nas laterais

            displayedHeight =
                size.height

            displayedWidth =
                displayedHeight *
                        imageAspectRatio

            offsetX =
                (size.width - displayedWidth) /
                        2f

            offsetY = 0f

        } else {

            // sobra espaço em cima/baixo

            displayedWidth =
                size.width

            displayedHeight =
                displayedWidth /
                        imageAspectRatio

            offsetX = 0f

            offsetY =
                (size.height - displayedHeight) /
                        2f
        }


        // ==============================
        // LINHA DE CONTAGEM
        // ==============================

        val lineScreenY =
            offsetY +
                    lineY *
                    displayedHeight

        drawLine(
            color = Color.Green,

            start = Offset(
                offsetX,
                lineScreenY
            ),

            end = Offset(
                offsetX + displayedWidth,
                lineScreenY
            ),

            strokeWidth = 6f
        )


        // ==============================
        // OBJETOS RASTREADOS
        // ==============================

        tracks.forEach { track ->

            val detection =
                track.detection


            val left =
                offsetX +
                        detection.x1 *
                        displayedWidth

            val top =
                offsetY +
                        detection.y1 *
                        displayedHeight

            val right =
                offsetX +
                        detection.x2 *
                        displayedWidth

            val bottom =
                offsetY +
                        detection.y2 *
                        displayedHeight


            // ==============================
            // BOUNDING BOX
            // ==============================

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


            // ==============================
            // TEXTO:
            //
            // ID 3 - 87%
            // ==============================

            drawContext
                .canvas
                .nativeCanvas
                .drawText(

                    "ID ${track.id} - ${
                        (
                                detection.confidence *
                                        100
                                ).toInt()
                    }%",

                    left,

                    (top - 10f)
                        .coerceAtLeast(40f),

                    android.graphics.Paint()
                        .apply {

                            color =
                                android.graphics.Color.RED

                            textSize = 40f

                            isAntiAlias = true
                        }
                )


            // ==============================
            // CENTRO DO OBJETO
            //
            // útil para enxergar exatamente
            // qual ponto usamos na contagem
            // ==============================

            val centerScreenX =
                offsetX +
                        track.centerX *
                        displayedWidth

            val centerScreenY =
                offsetY +
                        track.centerY *
                        displayedHeight


            drawCircle(
                color = Color.Yellow,

                radius = 8f,

                center = Offset(
                    centerScreenX,
                    centerScreenY
                )
            )
        }
    }
}