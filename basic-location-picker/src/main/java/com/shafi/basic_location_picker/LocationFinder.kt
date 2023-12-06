package com.shafi.basic_location_picker

import android.annotation.SuppressLint
import android.app.Activity
import android.content.IntentSender
import android.location.Location
import android.os.Handler
import android.os.Looper
import androidx.activity.result.ActivityResultLauncher
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.google.android.gms.tasks.CancellationTokenSource
import com.google.android.gms.tasks.Task
import com.shafi.basic_location_picker.LocationHelper.isLocationPermissionGranted
import com.shafi.basic_location_picker.LocationHelper.permissions

/**
 * Created by Shafi on 7/18/2020.
 */

class LocationFinder(
    private var activity: Activity,
    private var permissionContracts: ActivityResultLauncher<Array<String>>,
    private var locationListener: LocationListener,
    private var isHighAccuracy: Boolean
) {
    private val TAG = LocationFinder::class.java.simpleName

    private lateinit var locationRequest: LocationRequest
    private val fusedLocationProviderClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(activity)
    private var basicLocationPickerProgressDialog: BasicLocationPickerProgressDialogWithMessage? =
        null

    companion object {

        public fun getInstance(
            activity: Activity,
            permissionContracts: ActivityResultLauncher<Array<String>>,
            locationListener: LocationListener,
            isHighAccuracy: Boolean
        ): LocationFinder {
            return LocationFinder(activity, permissionContracts, locationListener, isHighAccuracy)
        }
    }

    init {
        basicLocationPickerProgressDialog = BasicLocationPickerProgressDialogWithMessage(activity)
        basicLocationPickerProgressDialog?.setMessage(activity.getString(R.string.getting_location))
    }

    public fun showProgressDialog() {
        basicLocationPickerProgressDialog?.show()
    }

    public fun dismissProgressDialog() {
        basicLocationPickerProgressDialog?.dismiss()
    }

    @SuppressLint("MissingPermission")
    public fun getLocation() {

        if (isLocationPermissionGranted(activity)) {

            showProgressDialog()
            if (isHighAccuracy) {
                initLocationRequest()
            } else {
                fusedLocationProviderClient.getCurrentLocation(
                    Priority.PRIORITY_HIGH_ACCURACY,
                    CancellationTokenSource().token
                ).addOnSuccessListener { location ->
                    if (location != null) {
                        Handler(Looper.getMainLooper()).postDelayed({
                            dismissProgressDialog()
                            stopLocationUpdates()
                            locationListener.onLocationFound(location)
                        }, 1000)
                    } else {
                        initLocationRequest()
                    }
                }
            }
        } else {
            permissionContracts.launch(permissions)
        }
    }

    private fun initLocationRequest() {
        try {

            locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 9000)
                .setWaitForAccurateLocation(true)
                .setMinUpdateIntervalMillis(5000)
                .build()

            val builder: LocationSettingsRequest.Builder =
                LocationSettingsRequest.Builder().addLocationRequest(locationRequest)
            builder.setAlwaysShow(true)

            val task: Task<LocationSettingsResponse> =
                LocationServices.getSettingsClient(activity).checkLocationSettings(builder.build())
            task.addOnSuccessListener { startLocationUpdates() }

            task.addOnFailureListener(activity) { exception ->
                dismissProgressDialog()
                if (exception is ResolvableApiException) {
                    try {
                        // Show the dialog by calling startResolutionForResult(),
                        // and check the result in onActivityResult().
                        exception.startResolutionForResult(
                            activity,
                            1356
                        )
                    } catch (sendEx: IntentSender.SendIntentException) {
                        // Ignore the error.
                    }
                }
            }

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun startLocationUpdates() {
        try {
            fusedLocationProviderClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                Looper.getMainLooper()
            )
        } catch (e: SecurityException) {
            e.printStackTrace()
        }
    }

    private val locationCallback: LocationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {
            super.onLocationResult(locationResult)
            for (location in locationResult.locations) {
                if (location != null) {
                    dismissProgressDialog()
                    stopLocationUpdates()
                    locationListener.onLocationFound(location)
                    break
                }
            }
        }
    }

    fun stopLocationUpdates() {
        try {
            fusedLocationProviderClient.removeLocationUpdates(locationCallback)
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        }
    }

    interface LocationListener {
        fun onLocationFound(location: Location)
    }
}