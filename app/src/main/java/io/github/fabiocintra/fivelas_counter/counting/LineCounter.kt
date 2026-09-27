package io.github.fabiocintra.fivelas_counter.counting

import io.github.fabiocintra.fivelas_counter.tracking.TrackedObject

class LineCounter(
    val lineY: Float = 0.60f
) {

    private val previousY =
        mutableMapOf<Int, Float>()

    private val countedIds =
        mutableSetOf<Int>()

    var count = 0
        private set


    fun update(
        tracks: List<TrackedObject>
    ) {

        tracks.forEach { track ->

            val oldY =
                previousY[track.id]

            val currentY =
                track.centerY

            if (oldY != null) {

                // Movimento de cima para baixo
                val crossed =
                    oldY < lineY &&
                            currentY >= lineY

                if (
                    crossed &&
                    track.id !in countedIds
                ) {

                    count++

                    countedIds.add(
                        track.id
                    )
                }
            }

            previousY[track.id] =
                currentY
        }
    }


    fun reset() {

        count = 0

        countedIds.clear()

        previousY.clear()
    }
}