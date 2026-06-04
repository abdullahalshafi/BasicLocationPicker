package com.shafi.basic_location_picker

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.os.Build
import android.os.Handler
import android.os.Looper
import androidx.activity.result.ActivityResultLauncher
import androidx.core.content.ContextCompat
import java.util.Locale
import java.util.concurrent.Executors

object LocationHelper {

    const val PACKAGE_NAME = "package_name"
    const val LOCATION_RESULT = "location_result"
    const val LOCATION_CONFIG = "location_config"
    const val LOCATION_FAIL_REASON = "location_fail_reason"

    val permissions = arrayOf(
        Manifest.permission.ACCESS_COARSE_LOCATION,
        Manifest.permission.ACCESS_FINE_LOCATION,
    )

    fun start(
        context: Context,
        intentLauncher: ActivityResultLauncher<Intent>,
        config: LocationRequestConfig = LocationRequestConfig.DEFAULT,
    ) {
        val intent = Intent(context, LocationHelperActivity::class.java).apply {
            putExtra(PACKAGE_NAME, context.packageName)
            putExtra(LOCATION_CONFIG, config)
        }
        intentLauncher.launch(intent)
    }

    fun isLocationPermissionGranted(context: Context): Boolean {
        val fine = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION,
        ) == PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_COARSE_LOCATION,
        ) == PackageManager.PERMISSION_GRANTED
        return fine && coarse
    }

    fun getAddressFromLocation(
        context: Context,
        location: Location,
        locale: Locale = Locale.ENGLISH,
        callback: (String?) -> Unit,
    ) {
        val geocoder = Geocoder(context, locale)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            geocoder.getFromLocation(location.latitude, location.longitude, 1) { addresses ->
                callback(addresses.firstOrNull()?.getAddressLine(0))
            }
            return
        }

        // Pre-Tiramisu: getFromLocation is synchronous and blocks. Run it off the main thread
        // and post the result back to the caller's looper (which is the main thread here).
        val mainHandler = Handler(Looper.getMainLooper())
        geocoderExecutor.execute {
            val address: String? = try {
                @Suppress("DEPRECATION")
                geocoder.getFromLocation(location.latitude, location.longitude, 1)
                    ?.firstOrNull()
                    ?.getAddressLine(0)
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
            mainHandler.post { callback(address) }
        }
    }

    private val geocoderExecutor = Executors.newSingleThreadExecutor { runnable ->
        Thread(runnable, "BasicLocationPicker-Geocoder").apply { isDaemon = true }
    }
}
