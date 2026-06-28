# Basic Location Picker

A simple, accuracy-aware Android library for getting the device's current location.

It wraps `FusedLocationProviderClient` with the things you usually have to write yourself: best-sample selection, an accuracy threshold, a hard timeout, staleness rejection, and optional mock-location filtering — all behind one call.

---

## Install

### Step 1 — Add the JitPack repository

In your root `settings.gradle` (or `build.gradle`):

```groovy
dependencyResolutionManagement {
    repositories {
        // ...
        maven { url 'https://jitpack.io' }
    }
}
```

### Step 2 — Add the dependency

```groovy
dependencies {
    implementation 'com.github.abdullahalshafi:BasicLocationPicker:2.0.2'
}
```

> **Upgrading from 1.x?** v2 is a breaking change — see [Migration from 1.x](#migration-from-1x) below.

---

## Quick start

```kotlin
class MainActivity : AppCompatActivity() {

    private val launcher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { activityResult ->
        if (activityResult.resultCode != Activity.RESULT_OK) {
            val reason = activityResult.data
                ?.getStringExtra(LocationHelper.LOCATION_FAIL_REASON)
            // reason is the name of a LocationFailReason enum value
            return@registerForActivityResult
        }

        val result: BasicLocationResult? =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                activityResult.data?.getParcelableExtra(
                    LocationHelper.LOCATION_RESULT,
                    BasicLocationResult::class.java,
                )
            } else {
                @Suppress("DEPRECATION")
                activityResult.data?.getParcelableExtra(LocationHelper.LOCATION_RESULT)
            }

        result?.let {
            Log.d("LOC", "lat=${it.location.latitude}, " +
                         "lng=${it.location.longitude}, " +
                         "accuracy=${it.accuracyMeters}m, " +
                         "samples=${it.sampleCount}, " +
                         "ttf=${it.timeToFixMs}ms, " +
                         "thresholdMet=${it.thresholdMet}, " +
                         "mock=${it.isMock}")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        findViewById<Button>(R.id.location_btn).setOnClickListener {
            LocationHelper.start(this, launcher)
        }
    }
}
```

That's it. `LocationHelper.start()` uses sensible defaults: target accuracy 20 m, 30 s timeout, mock locations allowed.

---

## Tuning accuracy and timeout

Pass a `LocationRequestConfig` when you need to override the defaults:

```kotlin
val config = LocationRequestConfig(
    targetAccuracyMeters = 15f,     // return as soon as a sample is this accurate
    timeoutMs = 30_000L,            // hard cap; returns best-so-far if not met
    maxLocationAgeMs = 10_000L,     // reject fixes older than this (elapsed-realtime)
    pollingIntervalMs = 1_000L,     // how often to poll GPS while converging
    minUpdateIntervalMs = 500L,     // floor on update rate
    rejectMockLocations = false,    // set true to refuse mock-provider fixes
    minDialogVisibleMs = 1_000L,    // dialog stays visible at least this long
)

LocationHelper.start(this, launcher, config)
```

### Picking a target accuracy

| Use case                       | `targetAccuracyMeters` |
|--------------------------------|-----------------------:|
| Ride-hailing / pickup pin      | 10 – 15                |
| Delivery proof / attendance    | 15 – 25                |
| Weather / general "where am I" | 30 – 50                |
| City-level                     | 100+                   |

Smaller targets mean longer waits (or hitting the timeout). Outdoor GPS reliably hits 5–15 m; indoors you'll often only get 30–100 m and should design for `thresholdMet = false`.

---

## Result shape

```kotlin
@Parcelize
data class BasicLocationResult(
    val location: Location,
    val accuracyMeters: Float,
    val isMock: Boolean,
    val sampleCount: Int,      // how many valid samples were considered
    val timeToFixMs: Long,     // wall-clock time from start to delivery
    val thresholdMet: Boolean, // false if returned best-effort at timeout
) : Parcelable
```

Inspect `thresholdMet` before trusting the fix for accuracy-sensitive operations.

## Failure reasons

When `resultCode != RESULT_OK`, read `LOCATION_FAIL_REASON` from the result intent — it's the `name` of one of:

```kotlin
enum class LocationFailReason {
    PERMISSION_DENIED,
    GPS_DISABLED,
    TIMEOUT_NO_SAMPLES,   // timeout elapsed without a single valid sample
    MOCK_REJECTED,        // rejectMockLocations=true and only mock fixes arrived
    ERROR,
}
```

---

## Reverse-geocoding (address lookup)

`getAddressFromLocation` is **callback-based** to avoid blocking the main thread (uses the async `Geocoder` API on Android 33+, a background thread on older devices):

```kotlin
LocationHelper.getAddressFromLocation(context, result.location) { address ->
    // address is the first line of the geocoded address, or null
}
```

Pass a `Locale` as the third arg if you need something other than `Locale.ENGLISH`.

---

## Permissions

The library declares `ACCESS_FINE_LOCATION` and `ACCESS_COARSE_LOCATION` in its manifest — no extra setup needed in your app's manifest.

Runtime permission requests and the GPS-enable prompt are handled internally. If the user denies either, the result is `RESULT_CANCELED` with `LOCATION_FAIL_REASON = PERMISSION_DENIED` or `GPS_DISABLED`.

You can also check permission state directly:

```kotlin
if (LocationHelper.isLocationPermissionGranted(context)) { ... }
```

---

## Migration from 1.x

| 1.x                                          | 2.0                                                    |
|----------------------------------------------|--------------------------------------------------------|
| `LocationHelper.start(ctx, launcher, true)`  | `LocationHelper.start(ctx, launcher, LocationRequestConfig(...))` |
| `result.data.getParcelableExtra<Location>(LOCATION_RESULT)` | `result.data.getParcelableExtra(LOCATION_RESULT, BasicLocationResult::class.java)` — then `.location` for the `Location` |
| `result.data.getBooleanExtra(IS_MOCK_LOCATION, false)` | `result.isMock` on `BasicLocationResult`              |
| `LocationHelper.getAddressFromLocation(ctx, loc)` returns `String?` (blocks) | `LocationHelper.getAddressFromLocation(ctx, loc) { address -> ... }` (async) |

The `isHighAccuracy: Boolean` flag is gone — v2 is always high-accuracy and lets you tune the trade-off precisely via the config.

---

## What v2 does differently under the hood

- **Picks the best sample**, not the most recent one — GPS gets more accurate as it locks more satellites, so the first sample is usually the worst.
- **Filters stale fixes** using `elapsedRealtimeNanos` so a cached "last known" fix doesn't masquerade as fresh.
- **Polls aggressively** (1 s default) while converging, then stops as soon as the threshold is met.
- **Hard timeout** with best-effort fallback — never spins forever on indoor cold-starts.
- **Validates `hasAccuracy()`** so a provider returning `0.0f` accuracy doesn't slip through as "perfect."
- **Leak-safe** — the internal `Handler` is cancelled on activity destroy.

---

## Changelog

### 2.0.2
- Add consumer ProGuard/R8 rules so minified consumer apps don't crash — keeps public types, `Parcelable` `CREATOR` fields, and `LocationFailReason` enum members.
- Harden config parsing: a malformed `LocationRequestConfig` extra now falls back to defaults instead of throwing.

### 2.0.1
- Fix mock-location rejection bug.

### 2.0.0
- Accuracy-aware rewrite: best-sample selection, accuracy threshold, hard timeout, staleness rejection, optional mock filtering. Breaking — see [Migration from 1.x](#migration-from-1x).

---

## License

No license has been specified yet — the library is provided as-is. Contact the author for usage terms.
