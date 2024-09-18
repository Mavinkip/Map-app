package com.example.mapapp
import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.provider.Settings
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import org.osmdroid.config.Configuration
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay

class MainActivity : AppCompatActivity(), LocationListener {
    private lateinit var map: MapView
    private lateinit var locationOverlay: MyLocationNewOverlay
    private lateinit var locationManager: LocationManager
    private lateinit var locationTextView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Configuration.getInstance().load(applicationContext, applicationContext.getSharedPreferences("osmdroid", MODE_PRIVATE))
        setContentView(R.layout.activity_main)

        map = findViewById(R.id.mapView)
        map.setMultiTouchControls(true)

        locationTextView = findViewById(R.id.locationTextView)

        // Initialize the LocationManager
        locationManager = getSystemService(LOCATION_SERVICE) as LocationManager

        // Check if location services are enabled
        if (!isLocationEnabled()) {
            promptEnableLocation()
        } else {
            // Request location permission
            requestLocationPermission()
        }
    }

    private fun isLocationEnabled(): Boolean {
        val locationMode: Int
        try {
            locationMode = Settings.Secure.getInt(contentResolver, Settings.Secure.LOCATION_MODE)
        } catch (e: Settings.SettingNotFoundException) {
            e.printStackTrace()
            return false
        }
        return locationMode != Settings.Secure.LOCATION_MODE_OFF
    }

    private fun promptEnableLocation() {
        Toast.makeText(this, "Please enable location services", Toast.LENGTH_LONG).show()
        val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
        startActivity(intent)
    }

    private fun requestLocationPermission() {
        val permissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted: Boolean ->
            if (isGranted) {
                getUserLocation()
            } else {
                Toast.makeText(this, "Location permission denied", Toast.LENGTH_SHORT).show()
            }
        }

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        } else {
            // Permission already granted
            getUserLocation()
        }
    }

    private fun getUserLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return
        }

        // Request location updates
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0L, 0f, this)
        locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0L, 0f, this)

        val location: Location? = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
            ?: locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)

        if (location != null) {
            updateMapLocation(location)
        } else {
            Toast.makeText(this, "Waiting for location update...", Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateMapLocation(location: Location) {
        val userLocation = GeoPoint(location.latitude, location.longitude)
        val mapController = map.controller
        mapController.setZoom(15.0)
        mapController.setCenter(userLocation)

        // Update the location TextView with the user's coordinates
        locationTextView.text = "Latitude: ${location.latitude}, Longitude: ${location.longitude}"

        // Add location overlay to show the user's current location
        locationOverlay = MyLocationNewOverlay(map)
        locationOverlay.enableMyLocation()
        map.overlays.add(locationOverlay)
    }

    override fun onLocationChanged(location: Location) {
        // Update map location when a new location is received
        updateMapLocation(location)
        // Remove location updates once the location is set
        locationManager.removeUpdates(this)
    }

    override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}

    override fun onProviderEnabled(provider: String) {}

    override fun onProviderDisabled(provider: String) {}
}
