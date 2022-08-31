package com.shafi.basic_location_picker

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.location.Location
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import com.shafi.basic_location_picker.LocationHelper.isLocationPermissionGranted
import com.shafi.basic_location_picker.LocationHelper.permissions

class LocationHelperActivity : AppCompatActivity(), LocationFinder.LocationListener {

    private var locationFinder: LocationFinder? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (isLocationPermissionGranted(this)) {
            handleLocationAfterPermissionGranter()
        } else {
            locationPermissionLauncher.launch(permissions)
        }
    }

    private fun handleLocationAfterPermissionGranter() {
        locationFinder = LocationFinder.getInstance(this, locationPermissionLauncher, this)
        if (locationFinder?.getCurrentLocation() == null) {
            locationFinder?.getLocation()
        } else {
            onLocationFound(locationFinder?.getCurrentLocation()!!)
        }
    }

    private fun requestLocationPermission() {
        when {
            isLocationPermissionGranted(this) -> {
                handleLocationAfterPermissionGranter()
            }
            ActivityCompat.shouldShowRequestPermissionRationale(
                this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) -> {
                showPermissionAlert(
                    getString(R.string.location_permission),
                    getString(R.string.give_location_permission),
                    getString(R.string.please_give_location_permission)
                ) { locationPermissionLauncher.launch(permissions) }
            }
            else -> {

                showPermissionAlert(
                    getString(R.string.location_permission),
                    getString(R.string.give_location_permission),
                    getString(R.string.please_give_location_permission)
                ) {
                    val packageName = intent.getStringExtra(LocationHelper.PACKAGE_NAME)
                    finish()
                    val intent = Intent()
                    intent.action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                    val uri = Uri.fromParts(
                        "package",
                        packageName, null
                    )
                    intent.data = uri
                    //intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    startActivity(intent)
                }

            }
        }
    }

    //location permission launcher
    private val locationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissionStatusMap ->

            if (!permissionStatusMap.containsValue(false)) {
                handleLocationAfterPermissionGranter()
            } else {
                requestLocationPermission()
            }
        }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        //if gps access is denied show error toast
        if (requestCode == 1356) {

            if (resultCode == Activity.RESULT_CANCELED) {
                locationFinder?.stopLocationUpdates()
                Toast.makeText(this, getString(R.string.enable_gps), Toast.LENGTH_SHORT).show()
                sendResultCanceledAndFinish(false)
            } else {
                locationFinder?.getLocation()
            }
        }
    }

    override fun onLocationFound(location: Location) {
        val intent = Intent()
        intent.putExtra(LocationHelper.LOCATION_RESULT, location)
        setResult(RESULT_OK, intent)
        finish()
    }

    //some error occurred
    private fun sendResultCanceledAndFinish(
        showToast: Boolean,
        message: String = getString(R.string.something_went_wrong_please_try_again)
    ) {
        if (showToast) {
            Toast.makeText(this, message, Toast.LENGTH_SHORT)
                .show()
        }
        setResult(Activity.RESULT_CANCELED)
        finish()
    }

    private fun showPermissionAlert(
        title: String,
        message: String,
        cancelMessage: String,
        function: () -> Unit
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
                sendResultCanceledAndFinish(true, cancelMessage)
            }
            .setCancelable(false)
            .show()
    }

    override fun onDestroy() {
        super.onDestroy()
        locationFinder = null
    }
}