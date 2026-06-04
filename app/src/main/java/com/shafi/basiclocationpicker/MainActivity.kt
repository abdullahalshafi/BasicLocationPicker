package com.shafi.basiclocationpicker

import android.app.Activity
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.shafi.basic_location_picker.BasicLocationResult
import com.shafi.basic_location_picker.LocationHelper
import com.shafi.basic_location_picker.LocationRequestConfig

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        findViewById<Button>(R.id.location_btn).setOnClickListener {
            val config = LocationRequestConfig(
                targetAccuracyMeters = 20f,
                timeoutMs = 30_000L,
                rejectMockLocations = true,
            )
            LocationHelper.start(this, launcher, config)
        }
    }

    private val launcher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { activityResult ->
            if (activityResult.resultCode != Activity.RESULT_OK) {
                val reason = activityResult.data?.getStringExtra(LocationHelper.LOCATION_FAIL_REASON)
                Log.d(TAG, "location failed: $reason")
                Toast.makeText(this, "Location failed: $reason", Toast.LENGTH_LONG).show()
                return@registerForActivityResult
            }

            val data = activityResult.data ?: return@registerForActivityResult
            val result: BasicLocationResult? =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    data.getParcelableExtra(LocationHelper.LOCATION_RESULT, BasicLocationResult::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    data.getParcelableExtra(LocationHelper.LOCATION_RESULT)
                }
            if (result == null) return@registerForActivityResult

            val loc = result.location
            Toast.makeText(
                this,
                "lat=${loc.latitude}, lng=${loc.longitude}, acc=${result.accuracyMeters}m",
                Toast.LENGTH_LONG,
            ).show()
            Log.d(
                TAG,
                "fix: lat=${loc.latitude}, lng=${loc.longitude}, acc=${result.accuracyMeters}m, " +
                    "samples=${result.sampleCount}, ttf=${result.timeToFixMs}ms, " +
                    "thresholdMet=${result.thresholdMet}, mock=${result.isMock}",
            )

            LocationHelper.getAddressFromLocation(this, loc) { address ->
                Toast.makeText(this, "address: $address", Toast.LENGTH_LONG).show()
                Log.d(TAG, "address: $address")
            }
        }

    companion object {
        private const val TAG = "BASIC_LOCATION_RESULT"
    }
}
