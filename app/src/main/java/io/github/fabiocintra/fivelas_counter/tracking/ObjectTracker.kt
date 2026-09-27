package io.github.fabiocintra.fivelas_counter.tracking

import io.github.fabiocintra.fivelas_counter.detection.Detection

data class TrackedObject(
    val id: Int,
    val detection: Detection,
    val centerX: Float,
    val centerY: Float
)