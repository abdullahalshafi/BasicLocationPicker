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
import com.google.android.gms.tasks.Task
import com.shafi.basic_location_picker.LocationHelper.isLocationPermissionGranted
import com.shafi.basic_location_picker.LocationHelper.permissions

/**
 * Created by Shafi on 7/18/2020.
 */

class LocationFinder(
    private var activity: Activity,
    private var permissionContracts: ActivityResultLauncher<Array<String>>,
    private var locationListener: LocationListener
) {
    private val TAG = LocationFinder::class.java.simpleName

    private lateinit var locationRequest: LocationRequest
    private val fusedLocationProviderClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(activity)
    private var progressDialog: ProgressDialogWithMessage? = null
    private var currentLocation: Location? = null

    companion object {

        public fun getInstance(
            activity: Activity,
            permissionContracts: ActivityResultLauncher<Array<String>>,
            locationListener: LocationListener
        ): LocationFinder {
            return LocationFinder(activity, permissionContracts, locationListener)
        }
    }

    init {
        progressDialog = ProgressDialogWithMessage(activity)
        progressDialog?.setMessage(activity.getString(R.string.getting_location))
    }

    public fun showProgressDialog() {
        progressDialog?.show()
    }

    public fun dismissProgressDialog() {
        progressDialog?.dismiss()
    }

    public fun getCurrentLocation(): Location? {
        return currentLocation
    }

    @SuppressLint("MissingPermission")
    public fun getLocation() {
        if (isLocationPermissionGranted(activity)) {
            showProgressDialog()
            fusedLocationProviderClient.lastLocation.addOnSuccessListener { location: Location? ->
                if (location != null) {
                    Handler(Looper.getMainLooper()).postDelayed({
                        currentLocation = location
                        dismissProgressDialog()
                        stopLocationUpdates()
                        locationListener.onLocationFound(location)
                    }, 1000)

                } else {
                    initLocationRequest()
                }
            }
        } else {
            permissionContracts.launch(permissions)
        }
    }

    private fun initLocationRequest() {
        try {
            locationRequest = LocationRequest.create()
            locationRequest.priority = Priority.PRIORITY_HIGH_ACCURACY
            locationRequest.interval = 45000
            locationRequest.fastestInterval = 30000

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
//            getLocation("startLocationUpdates")
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
                    getLocation()
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