package com.example.mymapapp

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.graphics.Color
import android.location.Address
import android.location.Geocoder
import android.location.Location
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import androidx.core.app.ActivityCompat
import com.example.mymapapp.databinding.ActivityMapsBinding
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices

import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.PolylineOptions
import java.util.Locale


import com.google.android.gms.maps.model.Polyline
//import com.google.maps.android.PolyUtil
import kotlinx.coroutines.*

import com.google.maps.GeoApiContext


class MapsActivity : AppCompatActivity(), OnMapReadyCallback {
    private lateinit var myMap: GoogleMap
    lateinit var binding : ActivityMapsBinding
    private var originMarker: Marker? = null
    private var destinationMarker: Marker? = null
    lateinit var coordinates : LatLng
    lateinit var marker:Marker
    private lateinit var directionsClient: GeoApiContext
    var polyline: Polyline? =null
    private lateinit var originAddress: String
    private lateinit var destinationAddress: String
    lateinit var currentAddress : String


    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startLocationUpdates()
            } else {
                Toast.makeText(this, "Location permission denied", Toast.LENGTH_SHORT).show()
            }
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMapsBinding.inflate(layoutInflater)
        setContentView(binding.root)




        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult?) {
                locationResult?.lastLocation?.let { location ->
                    // Handle location updates
                    reverseGeocodeLocation(location)
                }
            }
        }


//        originAddress = "45 NE 192th street miami gardens"
//        destinationAddress = "310 NW 207th street miami gardens"

//        setContentView(R.layout.activity_maps)

        val supportFragment =
            supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        supportFragment.getMapAsync(this)

        binding.materialSwitch.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                binding.txOrigin.isEnabled = false
                binding.txOrigin.setText(currentAddress)
            } else {
                binding.txOrigin.isEnabled = true
                binding.txOrigin.setText("")
            }

        }
    }

    override fun onMapReady(map: GoogleMap) {
        myMap = map


        binding.itineraryBtn.setOnClickListener {
            drawLine()
        }

    }

    override fun onStart() {
        super.onStart()
        if (hasLocationPermission()) {
            startLocationUpdates()
        } else {
            requestLocationPermission()
        }
    }

    override fun onStop() {
        super.onStop()
        stopLocationUpdates()
    }

    private fun hasLocationPermission(): Boolean {
        return PackageManager.PERMISSION_GRANTED == checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION)
    }

    private fun requestLocationPermission() {
        requestPermissions(arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION), LOCATION_PERMISSION_REQUEST_CODE)
    }

    private fun startLocationUpdates() {
        val locationRequest = LocationRequest.create().apply {
            interval = 30_000_000
            fastestInterval = 15_000_000
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        }
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return
        }
        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, null)
    }

    private fun stopLocationUpdates() {
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }

    private fun reverseGeocodeLocation(location: Location) {
        val geocoder = Geocoder(this, Locale.getDefault())
        coordinates = LatLng(location.latitude, location.longitude)
        val addresses: List<Address> =
            geocoder.getFromLocation(location.latitude, location.longitude, 1) as List<Address>
        if (addresses.isNotEmpty()) {
            val address = addresses[0]
            val addressLine = address.getAddressLine(0)
            currentAddress = addressLine
            displayPosition(coordinates)
            // Handle the address as needed
            showToast(addressLine)
//            displayPosition(LatLng(location.latitude, location.longitude))
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1001
    }

    fun displayPosition(bounds:LatLngBounds, padding:Int) {
        myMap.setOnMapLoadedCallback {
//            map.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds, padding))
            myMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, padding))
        }
    }
    fun displayPosition(currentLocation: LatLng) {
        myMap.setOnMapLoadedCallback {
            val zoomLvl = 17f
            marker = myMap.addMarker(MarkerOptions().position(currentLocation).title("Now"))!!
            val cameraUpdate = CameraUpdateFactory.newLatLngZoom(marker.position, zoomLvl)
            myMap.animateCamera(cameraUpdate)
        }
    }

    private fun drawLine() {
        val originAddress = binding.txOrigin.text.toString()
        val destinationAddress = binding.textDestination.text.toString()

        // Geocode origin and destination addresses to obtain LatLng coordinates
        val originLatLng = geocodeLocation(originAddress)
        val destinationLatLng = geocodeLocation(destinationAddress)


        // Add markers for origin and destination
        myMap.addMarker(MarkerOptions().position(originLatLng).title("Origin"))
        myMap.addMarker(MarkerOptions().position(destinationLatLng).title("Destination"))

        polyline?.remove()

        // Draw a polyline between origin and destination
        val polylineOptions = PolylineOptions()
            .add(originLatLng, destinationLatLng)
            .color(Color.BLUE)
            .width(5f)

        myMap.addPolyline(polylineOptions)

        val padding = resources.getDimensionPixelSize(R.dimen.map_padding)

        val bounds = LatLngBounds
            .builder()
            .include(originLatLng)
            .include(destinationLatLng)
            .build()

        myMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, padding))

    }

    private fun geocodeLocation(address: String): LatLng {
        // Geocoding to obtain LatLng coordinates for the address

        val geocoder = Geocoder(this)
        val addresses = geocoder.getFromLocationName(address, 1)
        if (addresses!!.isNotEmpty()) {
            val location = addresses!![0]
            return LatLng(location.latitude, location.longitude)
        }
        // Return default coordinates if geocoding fails
        Toast.makeText(this, "Not address found", Toast.LENGTH_LONG).show()
        return LatLng(0.0, 0.0)
    }

}

fun unUsed() {
    /*       val padding = resources.getDimensionPixelSize(R.dimen.map_padding)

//         Set up your map customization here
//
//         Move the camera to a specific location

        val location = LatLng(37.7749, -122.4194)
        myMap.moveCamera(CameraUpdateFactory.newLatLngZoom(location, 12f))

         Add origin and destination markers
        val origin = LatLng(37.7749, -122.4194)
        val destination = LatLng(37.7837, -122.4064)

        originMarker = myMap.addMarker(MarkerOptions().position(origin).title("Origin"))
        destinationMarker =
            myMap.addMarker(MarkerOptions().position(destination).title("Destination"))

        // Draw a polyline between origin and destination
        val polylineOptions = PolylineOptions()
            .add(origin, destination)
            .color(Color.BLUE)
            .width(5f)

        myMap.addPolyline(polylineOptions)

        val bounds = LatLngBounds
            .builder()
            .include(origin)
            .include(destination)
            .build()

        // Adjust the padding



        //Action When Map is Loaded

//        displayPosition(coordinates)
//        displayPosition(bounds, padding)
        myMap.setOnMapLoadedCallback {
//            map.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds, padding))
            myMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, padding))
        }*/



//        myMap.setOnMapLoadedCallback {
//            map.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds, padding))
//            myMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, padding))
//        }

    // Move the camera to the midpoint between origin and destination
//        val midpoint = LatLng(
//            (originLatLng.latitude + destinationLatLng.latitude) / 2,
//            (originLatLng.longitude + destinationLatLng.longitude) / 2
//        )
//        myMap.animateCamera(CameraUpdateFactory.newLatLngZoom(midpoint, 15f))
}

