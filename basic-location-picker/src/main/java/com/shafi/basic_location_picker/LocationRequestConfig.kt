package com.shafi.basic_location_picker

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class LocationRequestConfig(
    val targetAccuracyMeters: Float = 20f,
    val timeoutMs: Long = 30_000L,
    val maxLocationAgeMs: Long = 10_000L,
    val pollingIntervalMs: Long = 1_000L,
    val minUpdateIntervalMs: Long = 500L,
    val rejectMockLocations: Boolean = false,
    val minDialogVisibleMs: Long = 1_000L,
) : Parcelable {

    companion object {
        val DEFAULT = LocationRequestConfig()
    }
}
