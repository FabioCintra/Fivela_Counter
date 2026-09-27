package io.github.fabiocintra.fivelas_counter.tracking

import io.github.fabiocintra.fivelas_counter.detection.Detection
import kotlin.math.sqrt

class SimpleTracker(
    private val maxDistance: Float = 0.15f,
    private val maxMissedFrames: Int = 5
) {

    private data class InternalTrack(
        val id: Int,
        var detection: Detection,
        var centerX: Float,
        var centerY: Float,
        var missedFrames: Int = 0
    )

    private val tracks =
        mutableListOf<InternalTrack>()

    private var nextId = 1


    fun update(
        detections: List<Detection>
    ): List<TrackedObject> {

        // Todos começam como "não vistos" neste frame
        tracks.forEach {
            it.missedFrames++
        }

        val usedTracks =
            mutableSetOf<Int>()

        detections.forEach { detection ->

            val centerX =
                (detection.x1 + detection.x2) / 2f

            val centerY =
                (detection.y1 + detection.y2) / 2f

            var bestTrack: InternalTrack? = null
            var bestDistance = Float.MAX_VALUE

            tracks.forEach { track ->

                if (track.id in usedTracks) {
                    return@forEach
                }

                val dx =
                    centerX - track.centerX

                val dy =
                    centerY - track.centerY

                val distance =
                    sqrt(dx * dx + dy * dy)

                if (
                    distance < bestDistance &&
                    distance < maxDistance
                ) {
                    bestDistance = distance
                    bestTrack = track
                }
            }

            if (bestTrack != null) {

                bestTrack!!.detection =
                    detection

                bestTrack!!.centerX =
                    centerX

                bestTrack!!.centerY =
                    centerY

                bestTrack!!.missedFrames = 0

                usedTracks.add(
                    bestTrack!!.id
                )

            } else {

                val track =
                    InternalTrack(
                        id = nextId++,
                        detection = detection,
                        centerX = centerX,
                        centerY = centerY
                    )

                tracks.add(track)

                usedTracks.add(
                    track.id
                )
            }
        }

        tracks.removeAll {
            it.missedFrames >
                    maxMissedFrames
        }

        return tracks
            .filter {
                it.missedFrames == 0
            }
            .map {

                TrackedObject(
                    id = it.id,
                    detection = it.detection,
                    centerX = it.centerX,
                    centerY = it.centerY
                )
            }
    }
}