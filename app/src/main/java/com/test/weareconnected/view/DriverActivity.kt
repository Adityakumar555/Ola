package com.test.weareconnected.view

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.test.weareconnected.R
import com.test.weareconnected.databinding.ActivityDriverBinding
import com.test.weareconnected.notification.RideNotificationService
import com.test.weareconnected.utils.AppSharedPreferences

class DriverActivity : AppCompatActivity() {
    private lateinit var binding: ActivityDriverBinding
    private val appSharedPreferences by lazy { AppSharedPreferences.getInstance(this) }
    private val db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityDriverBinding.inflate(layoutInflater)
        setContentView(binding.root)


        // Set that this is the main activity visited
        appSharedPreferences?.setMainActivityVisited(true)

        val userType = appSharedPreferences?.getUserType()

        if (userType == "Driver") {

            val serviceIntent = Intent(this, RideNotificationService::class.java)
            startService(serviceIntent)

            // Hide the user layout and show a toast for driver
            Toast.makeText(this, "Driver hai", Toast.LENGTH_SHORT).show()

            // Show the dialog to driver when there is a ride request
            fetchNearbyRides() // This function will fetch the nearby rides for the driver
        }



        // Navigate to user profile details activity when profile button is clicked
        binding.profile.setOnClickListener {
            val intent = Intent(this, UserProfileDetailsActivity::class.java)
            startActivity(intent)
        }

    }

    // Updated fetchNearbyRides() method:
    private fun fetchNearbyRides() {
        val currentUserPhoneNumber = appSharedPreferences?.getMobileNumber()

        // Set up a real-time listener on the 'nearby_drivers' collection
        db.collection("nearby_drivers")
            .whereEqualTo("rideStatus", "pending")  // Optionally filter by ride status (if necessary)
            .addSnapshotListener { snapshot, exception ->
                if (exception != null) {
                    Toast.makeText(this, "Error fetching nearby drivers: ${exception.message}", Toast.LENGTH_SHORT).show()
                    return@addSnapshotListener
                }

                if (snapshot != null && !snapshot.isEmpty) {
                    val nearbyDrivers = mutableListOf<DocumentSnapshot>()

                    // Loop through the documents to filter out nearby drivers based on phone number match
                    for (driver in snapshot.documents) {
                        val driverPhoneNumber = driver.getString("number")

                        if (driverPhoneNumber == currentUserPhoneNumber) {
                            nearbyDrivers.add(driver)
                        }
                    }

                    // If matching drivers were found, show the ride request dialog to the driver
                    if (nearbyDrivers.isNotEmpty()) {
                        val fromLocation = nearbyDrivers[0].getString("fromLocation") ?: "Unknown"
                        val toLocation = nearbyDrivers[0].getString("toLocation") ?: "Unknown"
                        showRideRequestDialog(fromLocation, toLocation, nearbyDrivers)
                    } else {
                        Toast.makeText(this, "No matching drivers found.", Toast.LENGTH_SHORT).show()
                    }
                }
            }
    }



    // Show ride request dialog for the driver only
    private fun showRideRequestDialog(fromLocation: String, toLocation: String, drivers: List<DocumentSnapshot>) {
        val userPhoneNumber = appSharedPreferences?.getMobileNumber() // Get the current user's phone number from SharedPreferences
        val userType = appSharedPreferences?.getUserType() // Get the current user's type (Driver or User)

        // Ensure the user is a "Driver" and phone numbers match
        if (userType == "Driver") {
            drivers.forEach { driver ->
                val driverPhoneNumber = driver.getString("number") // Get the driver's phone number from Firestore
                val driverName = driver.getString("name") ?: "Unknown Driver"
                val rideRequestUserName = driver.getString("userName") ?: "Unknown Name"
                val rideRequestUserNumber = driver.getString("userPhoneNumber") ?: "Unknown Number"

                // Check if the driver's phone number matches the user's phone number
                if (driverPhoneNumber == userPhoneNumber && driverPhoneNumber != null) {
                    // If the phone numbers match, show the ride request dialog to the driver
                    val dialogBuilder = AlertDialog.Builder(this)

                    // Create the dialog
                    val dialog = dialogBuilder.setTitle("Ride Request")
                        .setMessage("User is requesting a ride from $fromLocation to $toLocation.\n\nDo you want to accept the ride?")
                        .setPositiveButton("Accept") { _, _ ->
                            // Handle the acceptance logic
                            handleRideAcceptance(driverPhoneNumber,rideRequestUserName,rideRequestUserNumber, fromLocation, toLocation)
                            removeRideRequest(driverPhoneNumber)
                        }
                        .setNegativeButton("Cancel") { _, _ ->
                            // Handle the rejection logic
                            handleRideRejection(driverPhoneNumber)
                            removeRideRequest(driverPhoneNumber)
                        }
                        .create() // Create the AlertDialog instance

                    dialog.show() // Show the dialog
                }
            }
        } else {
            Toast.makeText(this, "Only drivers can accept rides.", Toast.LENGTH_SHORT).show()
        }
    }

    // Handle driver's rejection of the ride
    private fun handleRideRejection(driverId: String) {
        // Notify the system that the driver rejected the ride
        Toast.makeText(this, "Driver $driverId rejected the ride", Toast.LENGTH_SHORT).show()
    }

    private fun handleRideAcceptance(
        driverPhoneNumber: String,
        requestedRideUserName: String,
        requestedRideUserNumber: String,
        fromLocation: String,
        toLocation: String
    ) {
        val currentUserPhoneNumber = appSharedPreferences?.getMobileNumber()

        if (currentUserPhoneNumber != null) {
            // Fetch driver details from Firestore
            db.collection("drivers")
                .document(driverPhoneNumber)
                .get()
                .addOnSuccessListener { driverDocument ->
                    if (driverDocument.exists()) {
                        val driverName = driverDocument.getString("name") ?: "Unknown Driver"

                        // Create ride request data
                        val rideRequestData = hashMapOf(
                            "requestedRideUserNumber" to requestedRideUserNumber,
                            "requestedRideUserName" to requestedRideUserName,
                            "userLocation" to fromLocation,
                            "toLocation" to toLocation,
                            "rideStatus" to "accepted",  // Ride status is 'accepted'
                            "rideAcceptedAt" to System.currentTimeMillis(),
                            "driverPhoneNumber" to driverPhoneNumber,
                            "driverName" to driverName,
                            "driverVehicleType" to driverDocument.getString("vehicleType"),
                            "driverLatitude" to driverDocument.getDouble("latitude"),
                            "driverLongitude" to driverDocument.getDouble("longitude")
                        )

                        // Save the ride request data to Firestore
                        db.collection("ride_requests")
                            .document(requestedRideUserNumber)
                            .set(rideRequestData)
                            .addOnSuccessListener {
                                Toast.makeText(this, "Ride accepted successfully!", Toast.LENGTH_LONG).show()
                                updateDriverStatusToOnRide(driverPhoneNumber)

                                // Now remove all other nearby drivers' ride requests
                                removeAllNearbyDriversRideRequests(driverPhoneNumber)

                                // Notify the user about the ride acceptance
                                // showToastToUserAboutRideAcceptance(requestedRideUserNumber, driverPhoneNumber)
                            }
                            .addOnFailureListener { e ->
                                Toast.makeText(this, "Failed to save ride request: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                    } else {
                        Toast.makeText(this, "Driver details not found.", Toast.LENGTH_SHORT).show()
                    }
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Error fetching driver details: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }

    // Function to remove all nearby drivers' ride requests after one driver accepts the ride
    private fun removeAllNearbyDriversRideRequests(acceptedDriverPhoneNumber: String) {
        db.collection("nearby_drivers")
            .whereEqualTo("rideStatus", "pending")  // Optionally filter by ride status
            .get()
            .addOnSuccessListener { snapshot ->
                if (!snapshot.isEmpty) {
                    // Loop through all nearby drivers and remove their ride requests
                    for (document in snapshot.documents) {
                        val driverPhoneNumber = document.getString("number")
                        if (driverPhoneNumber != null && driverPhoneNumber != acceptedDriverPhoneNumber) {
                            // Remove the ride request from 'nearby_drivers'
                            db.collection("nearby_drivers").document(document.id)
                                .delete()
                                .addOnSuccessListener {
                                    // Optionally, show a toast or log when a ride request is removed
                                    Toast.makeText(this, "Removed ride request for $driverPhoneNumber", Toast.LENGTH_SHORT).show()
                                }
                                .addOnFailureListener { e ->
                                    Toast.makeText(this, "Failed to remove ride request: ${e.message}", Toast.LENGTH_SHORT).show()
                                }
                        }
                    }
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error fetching nearby drivers: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }


    // Function to update driver status to "on ride"
    private fun updateDriverStatusToOnRide(driverPhoneNumber: String) {
        db.collection("drivers").document(driverPhoneNumber)
            .update("status", "on ride")
            .addOnSuccessListener {
                Toast.makeText(this, "Driver status updated to 'on ride'.", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Failed to update driver status: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }


    // Update ride request status in Firestore
    private fun removeRideRequest(rideId: String) {
        db.collection("nearby_drivers").document(rideId)
            .delete()
            .addOnSuccessListener {
                Toast.makeText(this, "Ride Remove", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to delete ride request.", Toast.LENGTH_SHORT).show()
            }
    }




}