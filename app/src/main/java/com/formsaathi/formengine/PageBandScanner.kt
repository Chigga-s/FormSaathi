package com.formsaathi.formengine

/**
 * Shared row scan that groups horizontal runs of printed pixels into rectangular
 * bands.
 *
 * Both printed underlines and printed answer boxes are bands; they differ only in
 * how thick they are and how dark they need to be. Keeping every qualifying run on
 * a row (not just the longest) is what lets two fields printed side by side on the
 * same row both be found — a footer with a date line and a signature line is the
 * common case.
 */
internal object PageBandScanner {

    data class Band(
        val top: Int,
        /** Exclusive. */
        val bottom: Int,
        val left: Int,
        /** Exclusive. */
        val right: Int
    ) {
        val height: Int get() = bottom - top
        val width: Int get() = right - left
    }

    private class OpenBand(val top: Int, var left: Int, var right: Int, var bottom: Int)

    /**
     * @param threshold a pixel counts as printed when its luminance is below this
     * @param minRun shortest horizontal run that can start or extend a band
     * @param overlapFraction how much of the shorter span two rows must share to
     *        belong to the same band
     * @param intersectColumns true keeps the columns common to every row (stays
     *        inside a printed rectangle), false keeps their union
     */
    fun scan(
        luminance: IntArray,
        width: Int,
        height: Int,
        threshold: Int,
        minRun: Int,
        overlapFraction: Float,
        intersectColumns: Boolean
    ): List<Band> {
        require(luminance.size == width * height) {
            "Luminance size ${luminance.size} != ${width}x$height"
        }
        if (width <= 0 || height <= 0) return emptyList()

        val bands = mutableListOf<Band>()
        var open = mutableListOf<OpenBand>()

        for (row in 0 until height) {
            val runs = runsOnRow(luminance, width, row, threshold, minRun)
            val stillOpen = mutableListOf<OpenBand>()
            val claimed = BooleanArray(runs.size)

            for (band in open) {
                var bestIndex = -1
                var bestOverlap = 0
                runs.forEachIndexed { index, run ->
                    if (claimed[index]) return@forEachIndexed
                    val overlap = minOf(band.right, run.second) - maxOf(band.left, run.first)
                    val shorter = minOf(band.right - band.left, run.second - run.first)
                    if (overlap > bestOverlap && overlap >= shorter * overlapFraction) {
                        bestOverlap = overlap
                        bestIndex = index
                    }
                }
                if (bestIndex < 0) {
                    bands.add(Band(band.top, band.bottom, band.left, band.right))
                    continue
                }
                claimed[bestIndex] = true
                val run = runs[bestIndex]
                if (intersectColumns) {
                    band.left = maxOf(band.left, run.first)
                    band.right = minOf(band.right, run.second)
                } else {
                    band.left = minOf(band.left, run.first)
                    band.right = maxOf(band.right, run.second)
                }
                band.bottom = row + 1
                stillOpen.add(band)
            }

            runs.forEachIndexed { index, run ->
                if (!claimed[index]) {
                    stillOpen.add(OpenBand(row, run.first, run.second, row + 1))
                }
            }
            open = stillOpen
        }

        open.forEach { bands.add(Band(it.top, it.bottom, it.left, it.right)) }
        return bands.sortedWith(compareBy({ it.top }, { it.left }))
    }

    /** All runs of printed pixels on [row] that are at least [minRun] wide. */
    private fun runsOnRow(
        luminance: IntArray,
        width: Int,
        row: Int,
        threshold: Int,
        minRun: Int
    ): List<Pair<Int, Int>> {
        val base = row * width
        val runs = mutableListOf<Pair<Int, Int>>()
        var start = -1
        for (x in 0..width) {
            val printed = x < width && luminance[base + x] < threshold
            if (printed) {
                if (start < 0) start = x
            } else if (start >= 0) {
                if (x - start >= minRun) runs.add(start to x)
                start = -1
            }
        }
        return runs
    }
}
