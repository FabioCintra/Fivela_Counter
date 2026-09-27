package io.github.fabiocintra.fivelas_counter.counting

class StableCounter(
    private val windowSize: Int = 10,
    private val minAgreement: Int = 7
) {

    private val history =
        ArrayDeque<Int>()

    var count: Int = 0
        private set


    fun update(
        currentCount: Int
    ): Int {

        history.addLast(currentCount)

        if (history.size > windowSize) {
            history.removeFirst()
        }

        // Ainda não temos frames suficientes
        if (history.size < minAgreement) {
            return count
        }

        // Descobre qual quantidade apareceu mais vezes
        val frequency =
            history
                .groupingBy { it }
                .eachCount()

        val mostCommon =
            frequency.maxByOrNull {
                it.value
            }

        if (
            mostCommon != null &&
            mostCommon.value >= minAgreement
        ) {
            count = mostCommon.key
        }

        return count
    }


    fun reset() {
        history.clear()
        count = 0
    }
}