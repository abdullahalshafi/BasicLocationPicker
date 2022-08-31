# Basic Location Picker
A Simple Android Library to get current location.

### Step 1. Add the JitPack repository to your build file
Add it in your root build.gradle at the end of repositories:
```Kotlin
allprojects {
    repositories {
        ...
        maven { url 'https://jitpack.io' }
    }
}
```

### Step 2. Add the dependency
```Kotlin
dependencies {
    implementation 'com.github.abdullahalshafi:BasicLocationPicker:1.0.0'
}
```

### Usage

```kotlin
   LocationHelper.start(this, launcher)
```

#### Location Result
```kotlin
private val launcher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val location = result.data?.getParcelableExtra<Location>(LocationHelper.LOCATION_RESULT) as Location
                Toast.makeText(this, "location: ${location.latitude} ${location.longitude}", Toast.LENGTH_LONG).show()
                Toast.makeText(this, "address: ${LocationHelper.getAddressFromLocation(this, location)}", Toast.LENGTH_LONG).show()
            }
        }
```


#### Get address from location using default Geocoder
```kotlin
val address = LocationHelper.getAddressFromLocation(this, location)
```


