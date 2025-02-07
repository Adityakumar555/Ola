package com.test.weareconnected.utils

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.LocationManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.util.Log
import android.util.Patterns
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.appcompat.app.AppCompatActivity.INPUT_METHOD_SERVICE
import androidx.appcompat.app.AppCompatActivity.LOCATION_SERVICE
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.location.LocationManagerCompat
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationSettingsRequest
import com.google.android.gms.location.Priority
import com.test.weareconnected.models.LocationData
import java.io.IOException
import java.util.Locale

class MyHelper(private val context: Context) {


    fun extractAddressDetails(latitude: Double, longitude: Double): LocationData? {
        try {
            val geocoder = Geocoder(context, Locale.getDefault())
            val addresses = geocoder.getFromLocation(latitude, longitude, 1)
            if (!addresses.isNullOrEmpty()) {
                val address = addresses[0]
                val fullAddress = address.getAddressLine(0) ?: "Unknown Address"

                // Split the full address into parts
                val addressParts = fullAddress.split(",").map { it.trim() }

                // Extract street and block
                val street = addressParts.getOrNull(0) ?: "Unknown Street"

                val block = if (addressParts.size > 1) {
                    val possibleBlock = addressParts[1]
                    if (possibleBlock.contains("Block", ignoreCase = true)) {
                        possibleBlock
                    } else {
                        addressParts[0]
                    }
                } else {
                    "Unknown Block"
                }

                val locality = address.subLocality ?: addressParts[2]
                val state = address.locality ?: addressParts[3]
                val subState = address.adminArea ?: "Unknown State"
                val postalCode = address.postalCode ?: "Unknown Postal Code"
                val country = address.countryName ?: "Unknown Country"

                Log.d("addressinMyhelper", "${addressParts[2]},${addressParts[1]}")

                Log.d("addressinMyhelper", "${fullAddress}")

                Log.d("addressinMyhelper", "${block},${street},${locality},${state},${subState},${postalCode},${country}")

                // Create a LocationData instance
                return LocationData(
                    fullAddress = fullAddress,
                    street = street,
                    block = block,
                    locality = locality,
                    state = state,
                    subState = subState,
                    postalCode = postalCode,
                    country = country
                )
            } else {
                return null
            }
        } catch (e: IOException) {
            e.printStackTrace()
            return null
        }
    }


    fun isOnline(): Boolean {
        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val activeNetwork = connectivityManager.activeNetwork
        val networkCapabilities = connectivityManager.getNetworkCapabilities(activeNetwork)
        return networkCapabilities != null && networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }


    fun requestLocationPermission(activity: Activity) {
        ActivityCompat.requestPermissions(
            activity, arrayOf(
                android.Manifest.permission.ACCESS_FINE_LOCATION,
                android.Manifest.permission.ACCESS_COARSE_LOCATION
            ), 10
        )
    }


    fun checkLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun isLocationEnable(): Boolean {
        val location: LocationManager =
            context.getSystemService(LOCATION_SERVICE) as LocationManager
        return LocationManagerCompat.isLocationEnabled(location)
    }

    fun onGPS(resultLauncher: ActivityResultLauncher<IntentSenderRequest>) {
        // Create a location request for high accuracy.
        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 3000)
            .setMinUpdateDistanceMeters(10f) // Update location when it changes by 10 meters
            .build()

        // Build location settings request
        val builder = LocationSettingsRequest.Builder().addLocationRequest(locationRequest)

        // Check location settings to see if GPS is enabled
        val task = LocationServices.getSettingsClient(context).checkLocationSettings(builder.build())

        task.addOnFailureListener { exception ->
            if (exception is ResolvableApiException) {
                try {
                    // If GPS is off, attempt to resolve it by prompting the user to enable it
                    val intentSenderRequest = IntentSenderRequest.Builder(exception.resolution).build()
                    resultLauncher.launch(intentSenderRequest) // Launch the prompt
                } catch (e: Exception) {
                    // Handle exceptions when launching the intent sender request
                    e.printStackTrace()
                    // Optionally, show a message indicating GPS could not be enabled
                    Toast.makeText(context, "Error enabling GPS", Toast.LENGTH_SHORT).show()
                }
            }
        }

        task.addOnSuccessListener {
            // GPS is already enabled, do nothing or show a message (optional)
            // Toast.makeText(context, "GPS is already enabled", Toast.LENGTH_SHORT).show()
        }
    }

     fun isValidPhoneNumber(phone: String): Boolean {
        return Patterns.PHONE.matcher(phone).matches()
    }

}