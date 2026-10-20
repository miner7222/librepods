package me.kavishdevar.librepods.data

internal const val EQ_BAND_COUNT = 8

/** Invalid saved state is rejected so the caller can use its existing defaults. */
internal fun restoreEqBands(values: List<Float>): FloatArray? =
    values.takeIf { it.size == EQ_BAND_COUNT }?.toFloatArray()
