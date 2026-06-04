package com.shafi.basic_location_picker

import android.annotation.SuppressLint
import android.app.Activity
import android.content.IntentSender
import android.location.Location
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import androidx.activity.result.ActivityResultLauncher
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.Granularity
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationSettingsRequest
import com.google.android.gms.location.LocationSettingsResponse
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.Task
import com.shafi.basic_location_picker.LocationHelper.isLocationPermissionGranted
import com.shafi.basic_location_picker.LocationHelper.permissions

class LocationFinder(
    private val activity: Activity,
    private val permissionContracts: ActivityResultLauncher<Array<String>>,
    private val locationListener: LocationListener,
    private val config: LocationRequestConfig,
) {

    private val fusedLocationProviderClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(activity)

    private val progressDialog: BasicLocationPickerProgressDialogWithMessage =
        BasicLocationPickerProgressDialogWithMessage(activity).apply {
            setMessage(activity.getString(R.string.getting_location))
        }

    private val uiHandler = Handler(Looper.getMainLooper())
    private val timeoutRunnable = Runnable { onTimeout() }
    private var pendingDelivery: Runnable? = null

    private var bestLocation: Location? = null
    private var sampleCount: Int = 0
    private var sawRejectedMock: Boolean = false
    private var startedAtElapsedMs: Long = 0L
    private var dialogShownAtElapsedMs: Long = 0L
    private var delivered: Boolean = false

    fun showProgressDialog() {
        progressDialog.show()
        dialogShownAtElapsedMs = SystemClock.elapsedRealtime()
    }

    fun dismissProgressDialog() = progressDialog.dismiss()

    @SuppressLint("MissingPermission")
    fun getLocation() {
        if (!isLocationPermissionGranted(activity)) {
            permissionContracts.launch(permissions)
            return
        }

        resetState()
        showProgressDialog()
        ensureSettingsAndStart()
    }

    private fun resetState() {
        bestLocation = null
        sampleCount = 0
        sawRejectedMock = false
        delivered = false
        startedAtElapsedMs = SystemClock.elapsedRealtime()
    }

    private fun buildLocationRequest(): LocationRequest =
        LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, config.pollingIntervalMs)
            .setMinUpdateIntervalMillis(config.minUpdateIntervalMs)
            .setGranularity(Granularity.GRANULARITY_FINE)
            .setWaitForAccurateLocation(true)
            .build()

    private fun ensureSettingsAndStart() {
        try {
            val request = buildLocationRequest()
            val settingsRequest = LocationSettingsRequest.Builder()
                .addLocationRequest(request)
                .setAlwaysShow(true)
                .build()

            val task: Task<LocationSettingsResponse> =
                LocationServices.getSettingsClient(activity).checkLocationSettings(settingsRequest)

            task.addOnSuccessListener { startLocationUpdates(request) }
            task.addOnFailureListener(activity) { exception ->
                dismissProgressDialog()
                if (exception is ResolvableApiException) {
                    try {
                        exception.startResolutionForResult(activity, GPS_RESOLUTION_REQUEST_CODE)
                    } catch (_: IntentSender.SendIntentException) {
                        deliverFailure(LocationFailReason.GPS_DISABLED)
                    }
                } else {
                    deliverFailure(LocationFailReason.GPS_DISABLED)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            deliverFailure(LocationFailReason.ERROR)
        }
    }

    @SuppressLint("MissingPermission")
    private fun startLocationUpdates(request: LocationRequest) {
        try {
            uiHandler.removeCallbacks(timeoutRunnable)
            uiHandler.postDelayed(timeoutRunnable, config.timeoutMs)

            fusedLocationProviderClient.requestLocationUpdates(
                request,
                locationCallback,
                Looper.getMainLooper(),
            )
        } catch (e: SecurityException) {
            e.printStackTrace()
            deliverFailure(LocationFailReason.PERMISSION_DENIED)
        }
    }

    private val locationCallback: LocationCallback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            result.locations.forEach { candidate ->
                considerSample(candidate)
            }
            maybeDeliverIfThresholdMet()
        }
    }

    private fun considerSample(candidate: Location) {
        if (!candidate.hasAccuracy()) return
        if (isStale(candidate)) return
        if (config.rejectMockLocations && candidate.isMockSafe()) {
            sawRejectedMock = true
            return
        }

        sampleCount++
        val current = bestLocation
        if (current == null || candidate.accuracy < current.accuracy) {
            bestLocation = candidate
        }
    }

    private fun maybeDeliverIfThresholdMet() {
        val best = bestLocation ?: return
        if (best.accuracy <= config.targetAccuracyMeters) {
            deliverSuccess(best, thresholdMet = true)
        }
    }

    private fun onTimeout() {
        val best = bestLocation
        if (best != null) {
            deliverSuccess(best, thresholdMet = false)
        } else if (sawRejectedMock) {
            deliverFailure(LocationFailReason.MOCK_REJECTED)
        } else {
            deliverFailure(LocationFailReason.TIMEOUT_NO_SAMPLES)
        }
    }

    private fun deliverSuccess(location: Location, thresholdMet: Boolean) {
        if (delivered || pendingDelivery != null) return

        // bestLocation is never a mock when rejectMockLocations is true (mocks are
        // filtered in considerSample), so this flag is only informational here.
        val isMock = location.isMockSafe()

        val result = BasicLocationResult(
            location = location,
            accuracyMeters = location.accuracy,
            isMock = isMock,
            sampleCount = sampleCount,
            timeToFixMs = SystemClock.elapsedRealtime() - startedAtElapsedMs,
            thresholdMet = thresholdMet,
        )
        scheduleDelivery { finishWithSuccess(result) }
    }

    private fun deliverFailure(reason: LocationFailReason) {
        if (delivered || pendingDelivery != null) return
        scheduleDelivery { finishWithFailure(reason) }
    }

    private fun scheduleDelivery(action: () -> Unit) {
        // Stop sampling immediately, but keep the progress dialog visible for at least
        // minDialogVisibleMs so it doesn't flash on instant fixes.
        stopLocationUpdates()

        val shownFor = SystemClock.elapsedRealtime() - dialogShownAtElapsedMs
        val remaining = (config.minDialogVisibleMs - shownFor).coerceAtLeast(0L)
        val runnable = Runnable {
            pendingDelivery = null
            action()
        }
        pendingDelivery = runnable
        if (remaining == 0L) runnable.run() else uiHandler.postDelayed(runnable, remaining)
    }

    private fun finishWithSuccess(result: BasicLocationResult) {
        delivered = true
        dismissProgressDialog()
        locationListener.onLocationFound(result)
    }

    private fun finishWithFailure(reason: LocationFailReason) {
        delivered = true
        dismissProgressDialog()
        locationListener.onLocationFailed(reason)
    }

    private fun isStale(location: Location): Boolean {
        val ageMs = (SystemClock.elapsedRealtimeNanos() - location.elapsedRealtimeNanos) / 1_000_000
        return ageMs > config.maxLocationAgeMs
    }

    private fun Location.isMockSafe(): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) isMock else isFromMockProvider

    fun stopLocationUpdates() {
        try {
            uiHandler.removeCallbacks(timeoutRunnable)
            fusedLocationProviderClient.removeLocationUpdates(locationCallback)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun release() {
        stopLocationUpdates()
        pendingDelivery?.let { uiHandler.removeCallbacks(it) }
        pendingDelivery = null
        dismissProgressDialog()
    }

    interface LocationListener {
        fun onLocationFound(result: BasicLocationResult)
        fun onLocationFailed(reason: LocationFailReason)
    }

    companion object {
        const val GPS_RESOLUTION_REQUEST_CODE = 1356

        fun getInstance(
            activity: Activity,
            permissionContracts: ActivityResultLauncher<Array<String>>,
            locationListener: LocationListener,
            config: LocationRequestConfig = LocationRequestConfig.DEFAULT,
        ): LocationFinder = LocationFinder(activity, permissionContracts, locationListener, config)
    }
}
