package com.shafi.basiclocationpicker

import android.app.Activity
import android.location.Location
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import com.shafi.basic_location_picker.LocationHelper

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        findViewById<Button>(R.id.location_btn).setOnClickListener {
            LocationHelper.start(this, launcher, true)
        }
    }

    private val launcher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val location = result.data?.getParcelableExtra<Location>(LocationHelper.LOCATION_RESULT) as Location
                Toast.makeText(this, "location: ${location.latitude} ${location.longitude}", Toast.LENGTH_LONG).show()
                Toast.makeText(this, "address: ${LocationHelper.getAddressFromLocation(this, location)}", Toast.LENGTH_LONG).show()
                Log.d("LOCATION", "result: ${location.latitude} ${location.longitude}")
                Log.d("LOCATION", "address: ${LocationHelper.getAddressFromLocation(this, location)}")
            }
        }
}