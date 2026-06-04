package com.shafi.basic_location_picker

import android.location.Location
import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class BasicLocationResult(
    val location: Location,
    val accuracyMeters: Float,
    val isMock: Boolean,
    val sampleCount: Int,
    val timeToFixMs: Long,
    val thresholdMet: Boolean,
) : Parcelable

enum class LocationFailReason {
    PERMISSION_DENIED,
    GPS_DISABLED,
    TIMEOUT_NO_SAMPLES,
    MOCK_REJECTED,
    ERROR,
}
