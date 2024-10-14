package com.shafi.basic_location_picker

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import androidx.activity.result.ActivityResultLauncher
import androidx.core.content.ContextCompat
import java.lang.Exception
import java.util.*

object LocationHelper {

    const val PACKAGE_NAME = "package_name"
    const val LOCATION_RESULT = "location_result"
    const val IS_HIGH_ACCURACY = "is_high_accuracy"
    const val IS_MOCK_LOCATION = "is_mock_location"

    val permissions = arrayOf(
        Manifest.permission.ACCESS_COARSE_LOCATION,
        Manifest.permission.ACCESS_FINE_LOCATION
    )

    fun start(
        context: Context,
        intentLauncher: ActivityResultLauncher<Intent>,
        isHighAccuracy: Boolean = false
    ) {

        val intent = Intent(context, LocationHelperActivity::class.java)
        intent.putExtra(PACKAGE_NAME, context.packageName)
        intent.putExtra(IS_HIGH_ACCURACY, isHighAccuracy)
        intentLauncher.launch(intent)
    }

    fun isLocationPermissionGranted(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
    }

    fun getAddressFromLocation(context: Context, location: Location): String? {
        return try {
            Geocoder(
                context,
                Locale.ENGLISH
            ).getFromLocation(location.latitude, location.longitude, 1)?.get(0)?.getAddressLine(0)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}