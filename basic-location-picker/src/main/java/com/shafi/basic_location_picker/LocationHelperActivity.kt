package com.shafi.basic_location_picker

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.shafi.basic_location_picker.LocationHelper.LOCATION_CONFIG
import com.shafi.basic_location_picker.LocationHelper.LOCATION_FAIL_REASON
import com.shafi.basic_location_picker.LocationHelper.LOCATION_RESULT
import com.shafi.basic_location_picker.LocationHelper.isLocationPermissionGranted
import com.shafi.basic_location_picker.LocationHelper.permissions

class LocationHelperActivity : AppCompatActivity(), LocationFinder.LocationListener {

    private var locationFinder: LocationFinder? = null
    private val config: LocationRequestConfig by lazy { readConfig() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (isLocationPermissionGranted(this)) {
            startLocationFlow()
        } else {
            locationPermissionLauncher.launch(permissions)
        }
    }

    private fun readConfig(): LocationRequestConfig {
        val fromIntent: LocationRequestConfig? =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                intent.getParcelableExtra(LOCATION_CONFIG, LocationRequestConfig::class.java)
            } else {
                @Suppress("DEPRECATION")
                intent.getParcelableExtra(LOCATION_CONFIG)
            }
        return fromIntent ?: LocationRequestConfig.DEFAULT
    }

    private fun startLocationFlow() {
        locationFinder = LocationFinder.getInstance(
            activity = this,
            permissionContracts = locationPermissionLauncher,
            locationListener = this,
            config = config,
        )
        locationFinder?.getLocation()
    }

    private fun promptForSettingsOrCancel() {
        if (isLocationPermissionGranted(this)) {
            startLocationFlow()
            return
        }

        showPermissionAlert(
            getString(R.string.location_permission),
            getString(R.string.give_location_permission),
            getString(R.string.please_give_location_permission),
        ) {
            val packageName = intent.getStringExtra(LocationHelper.PACKAGE_NAME)
            finishWithFailure(LocationFailReason.PERMISSION_DENIED, showToast = false)
            val settingsIntent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
            settingsIntent.data = Uri.fromParts("package", packageName, null)
            startActivity(settingsIntent)
        }
    }

    private val locationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
            if (!result.containsValue(false)) {
                startLocationFlow()
            } else {
                promptForSettingsOrCancel()
            }
        }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == LocationFinder.GPS_RESOLUTION_REQUEST_CODE) {
            if (resultCode == Activity.RESULT_CANCELED) {
                locationFinder?.stopLocationUpdates()
                Toast.makeText(this, getString(R.string.enable_gps), Toast.LENGTH_SHORT).show()
                finishWithFailure(LocationFailReason.GPS_DISABLED, showToast = false)
            } else {
                locationFinder?.getLocation()
            }
        }
    }

    override fun onLocationFound(result: BasicLocationResult) {
        val data = Intent().apply { putExtra(LOCATION_RESULT, result) }
        setResult(RESULT_OK, data)
        finish()
    }

    override fun onLocationFailed(reason: LocationFailReason) {
        finishWithFailure(reason, showToast = false)
    }

    private fun finishWithFailure(reason: LocationFailReason, showToast: Boolean, message: String? = null) {
        if (showToast) {
            val text = message ?: getString(R.string.something_went_wrong_please_try_again)
            Toast.makeText(this, text, Toast.LENGTH_SHORT).show()
        }
        val data = Intent().apply { putExtra(LOCATION_FAIL_REASON, reason.name) }
        setResult(Activity.RESULT_CANCELED, data)
        finish()
    }

    private fun showPermissionAlert(
        title: String,
        message: String,
        cancelMessage: String,
        function: () -> Unit,
    ) {
        AlertDialog.Builder(this)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton(getString(R.string.ok)) { dialogInterface, _ ->
                function.invoke()
                dialogInterface.dismiss()
            }
            .setNegativeButton(getString(R.string.cancel)) { dialogInterface, _ ->
                dialogInterface.dismiss()
                finishWithFailure(LocationFailReason.PERMISSION_DENIED, showToast = true, message = cancelMessage)
            }
            .setCancelable(false)
            .show()
    }

    override fun onDestroy() {
        locationFinder?.release()
        locationFinder = null
        super.onDestroy()
    }
}
