package com.test.weareconnected.view

import android.content.Intent
import android.location.Geocoder
import android.location.Location
import android.os.Bundle
import android.view.View
import android.widget.RadioButton
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.test.weareconnected.R
import com.test.weareconnected.databinding.ActivityMainBinding
import com.test.weareconnected.notification.RideNotificationService
import com.test.weareconnected.utils.AppSharedPreferences
import com.test.weareconnected.utils.MyHelper
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private val appSharedPreferences by lazy { AppSharedPreferences.getInstance(this) }
    private val myHelper by lazy { MyHelper(this) }

    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    private lateinit var gpsLauncher: ActivityResultLauncher<IntentSenderRequest>

    private lateinit var binding: ActivityMainBinding
    private val db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Set that this is the main activity visited
        appSharedPreferences?.setMainActivityVisited(true)

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)

        // Get the current location when activity starts
        getCurrentLocation()

        // Register the GPS setting prompt launcher
        gpsLauncher =
            registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
                if (result.resultCode == RESULT_OK) {
                    // If the user enables GPS, fetch the location
                    getCurrentLocation()
                } else {
                    // If user cancels or denies, inform the user
                    Toast.makeText(this, "GPS is required to proceed.", Toast.LENGTH_SHORT).show()
                }
            }

        val userType = appSharedPreferences?.getUserType()

        if (userType == "User") {
            val serviceIntent = Intent(this, RideNotificationService::class.java)
            startService(serviceIntent)

            // The existing user behavior here
            binding.userLayout.visibility = View.VISIBLE
            listenForRideStatusUpdate(appSharedPreferences?.getMobileNumber() ?: "")

            binding.bookRide.setOnClickListener {
                // When book ride is clicked, request the ride details from the user
                val fromLocation = binding.fromLocation.text.toString()
                val toLocation = binding.toLocation.text.toString()

                if (fromLocation.isNotEmpty() && toLocation.isNotEmpty()) {
                    // Save ride request to Firestore
                    // Fetch nearby drivers (dialog for nearby drivers will be shown)
                    fetchNearbyDrivers(fromLocation, toLocation)
                } else {
                    Toast.makeText(
                        this,
                        "Please enter both from and to locations.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }

            Toast.makeText(this, "User hai", Toast.LENGTH_SHORT).show()
        }


        // Navigate to user profile details activity when profile button is clicked
        binding.profile.setOnClickListener {
            val intent = Intent(this, UserProfileDetailsActivity::class.java)
            startActivity(intent)
        }
    }

    // Listen for ride status changes in Firestore (when the driver accepts the ride)
    private fun listenForRideStatusUpdate(requestedRideUserPhoneNumber: String) {
        db.collection("ride_requests")
            .whereEqualTo("requestedRideUserNumber", requestedRideUserPhoneNumber)
            .addSnapshotListener { rideRequestSnapshot, e ->
                if (e != null) {
                    Toast.makeText(
                        this,
                        "Error listening for ride request updates: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                    return@addSnapshotListener
                }

                rideRequestSnapshot?.documents?.firstOrNull()?.let { rideRequestDoc ->
                    val rideStatus = rideRequestDoc.getString("rideStatus")
                    val fromLocation =
                        rideRequestDoc.getString("userLocation") ?: "Unknown Location"
                    val toLocation = rideRequestDoc.getString("toLocation") ?: "Unknown Location"
                    val driverName = rideRequestDoc.getString("driverName") ?: "Unknown Driver"

                    // If ride status is 'accepted', show the notification
                    if (rideStatus == "accepted") {
                        Toast.makeText(
                            this,
                            "Your ride is accepted! Driver: $driverName, from $fromLocation to $toLocation.",
                            Toast.LENGTH_LONG
                        ).show()

                    }
                }
            }
    }


    // Function to fetch nearby drivers and show the dialog to nearby drivers
                private fun fetchNearbyDrivers(fromLocation: String, toLocation: String) {
                    fusedLocationProviderClient.lastLocation.addOnCompleteListener(this) { task ->
                        val location: Location? = task.result
                        if (location != null) {
                            val latitude = location.latitude
                            val longitude = location.longitude

                            // Get the selected vehicle type
                            val selectedVehicleType = getSelectedVehicleType()

                            if (selectedVehicleType != null) {
                                // Query Firestore to get drivers within a 200-meter radius
                                db.collection("drivers")
                                    .whereEqualTo("vehicleType", selectedVehicleType)
                                    .get()
                                    .addOnSuccessListener { result ->
                                        if (result.isEmpty) {
                                            Toast.makeText(
                                                this,
                                                "No nearby drivers found for this vehicle type.",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        } else {
                                            val nearbyDrivers = mutableListOf<DocumentSnapshot>()
                                            for (driver in result.documents) {
                                                val driverLat = driver.getDouble("latitude")
                                                val driverLon = driver.getDouble("longitude")

                                                if (driverLat != null && driverLon != null) {
                                                    val distance = calculateDistance(
                                                        latitude,
                                                        longitude,
                                                        driverLat,
                                                        driverLon
                                                    )
                                                    if (distance <= 2000) { // 200 meters distance
                                                        nearbyDrivers.add(driver)

                                                        // Save the nearby driver to Firestore under the specific ride request
                                                        saveNearbyDriverToDriverPhoneNumber(
                                                            driver,
                                                            fromLocation,
                                                            toLocation
                                                        )
                                                    }
                                                }
                                            }

                                            if (nearbyDrivers.isNotEmpty()) {
                                                Toast.makeText(
                                                    this,
                                                    "Driver fount please wait.",
                                                    Toast.LENGTH_SHORT
                                                ).show()

                                                // Show a dialog to each nearby driver
                                                //  showRideRequestDialog(fromLocation, toLocation, nearbyDrivers)
                                            } else {
                                                Toast.makeText(
                                                    this,
                                                    "No drivers found within 200 meters.",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            }
                                        }
                                    }
                            } else {
                                Toast.makeText(
                                    this,
                                    "Please select a vehicle type.",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    }
                }

                private fun saveNearbyDriverToDriverPhoneNumber(
                    driver: DocumentSnapshot,
                    fromLocation: String,
                    toLocation: String
                ) {
                    // Get the driver's contact number and other details
                    val driverPhoneNumber =
                        driver.getString("number") // Assuming you have "number" field in the driver's data
                    val driverName = driver.getString("name")
                    val driverVehicleType = driver.getString("vehicleType")
                    val driverLatitude = driver.getDouble("latitude")
                    val driverLongitude = driver.getDouble("longitude")

                    // Get the current user's details (you can also fetch user's name and phone number from SharedPreferences or Firebase)
                    val userPhoneNumber = appSharedPreferences?.getMobileNumber()
                    val userName =
                        appSharedPreferences?.getUserName()  // Assuming this is saved in shared preferences

                    // Check if the driver's phone number is available
                    if (driverPhoneNumber != null && userPhoneNumber != null && userName != null) {
                        // Construct the data to save under the 'nearby_drivers' sub-collection
                        val driverData = hashMapOf(
                            "driverId" to driver.id,
                            "name" to driverName,
                            "vehicleType" to driverVehicleType,
                            "latitude" to driverLatitude,
                            "longitude" to driverLongitude,
                            "number" to driverPhoneNumber, // Save the driver's phone number here
                            "rideStatus" to "pending",  // Add the default rideStatus as "pending"
                            "fromLocation" to fromLocation,  // Add the fromLocation
                            "toLocation" to toLocation,   // Add the toLocation
                            "userPhoneNumber" to userPhoneNumber,  // Save user's phone number
                            "userName" to userName  // Save user's name
                        )

                        // Save the nearby driver data under the 'nearby_drivers' collection
                        db.collection("nearby_drivers")  // Assuming you store driver data in the "nearby_drivers" collection
                            .document(driverPhoneNumber)  // Use the driver's phone number as the document ID
                            .set(driverData)  // Add the new driver data as a document under nearby_drivers
                            .addOnSuccessListener {
                                Toast.makeText(
                                    this,
                                    "Nearby driver saved successfully!",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                            .addOnFailureListener { e ->
                                Toast.makeText(
                                    this,
                                    "Failed to save nearby driver: ${e.message}",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                    } else {
                        Toast.makeText(
                            this,
                            "Driver phone number or user details are missing.",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }


                // Calculate distance using Haversine formula (in meters)
                private fun calculateDistance(
                    lat1: Double,
                    lon1: Double,
                    lat2: Double,
                    lon2: Double
                ): Float {
                    val earthRadius = 6371000 // meters
                    val dLat = Math.toRadians(lat2 - lat1)
                    val dLon = Math.toRadians(lon2 - lon1)
                    val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                            Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                            Math.sin(dLon / 2) * Math.sin(dLon / 2)
                    val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
                    return (earthRadius * c).toFloat()
                }


                // Function to get the selected vehicle type from the RadioGroup
                private fun getSelectedVehicleType(): String? {
                    val selectedId = binding.vehicleTypeGroup.checkedRadioButtonId
                    val selectedRadioButton = findViewById<RadioButton>(selectedId)

                    return when (selectedRadioButton?.id) {
                        R.id.bikeRadio -> "Bike"
                        R.id.autoRadio -> "Auto"
                        R.id.carRadio -> "Car"
                        else -> null
                    }
                }


                // Function to get current location
                private fun getCurrentLocation() {
                    if (myHelper.isLocationEnable()) {
                        if (myHelper.checkLocationPermission()) {
                            // Permission is granted, fetch the location
                            fusedLocationProviderClient.lastLocation.addOnCompleteListener(this) { task ->
                                val location: Location? = task.result
                                location?.let {
                                    val addressList =
                                        Geocoder(this, Locale.getDefault()).getFromLocation(
                                            it.latitude, it.longitude, 1
                                        )
                                    val address = addressList?.get(0)?.getAddressLine(0)
                                    binding.fromLocation.setText(address)

                                    // Save user address in shared preferences
                                    if (address != null) {
                                        appSharedPreferences?.saveUserAddress(address)
                                    }
                                }
                            }
                        } else {
                            // Request permission if not granted
                            myHelper.requestLocationPermission(this)
                        }
                    } else {
                        // If location services are disabled, request user to enable it
                        myHelper.onGPS(gpsLauncher)
                    }
                }
            }
