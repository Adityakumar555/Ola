package com.test.weareconnected.view

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.test.weareconnected.DriverRideClickListener
import com.test.weareconnected.databinding.ActivityDriverBinding
import com.test.weareconnected.models.DriverRides
import com.test.weareconnected.notification.RideNotificationService
import com.test.weareconnected.utils.AppSharedPreferences
import com.test.weareconnected.view.adapters.RideRequestAdapter
import java.util.UUID

class DriverActivity : AppCompatActivity(), DriverRideClickListener {
    private lateinit var binding: ActivityDriverBinding
    private val appSharedPreferences by lazy { AppSharedPreferences.getInstance(this) }
    private val db = FirebaseFirestore.getInstance()

    private lateinit var rideRequestAdapter: RideRequestAdapter
    private val rideRequestsList = mutableListOf<DriverRides>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityDriverBinding.inflate(layoutInflater)
        setContentView(binding.root)

        appSharedPreferences?.setMainActivityVisited(true)

        val userType = appSharedPreferences?.getUserType()

        if (userType == "Driver") {
            val serviceIntent = Intent(this, RideNotificationService::class.java)
            startService(serviceIntent)
            Toast.makeText(this, "Driver hai", Toast.LENGTH_SHORT).show()
        }

        binding.profile.setOnClickListener {
            val intent = Intent(this, UserProfileDetailsActivity::class.java)
            startActivity(intent)
        }

        getAllRideForCurrentDriver()
        rideRequestAdapter = RideRequestAdapter(rideRequestsList, this)
        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = rideRequestAdapter
    }

    override fun onStart() {
        super.onStart()
        fetchNearbyRides()
    }

    private fun fetchNearbyRides() {
        val currentUserPhoneNumber = appSharedPreferences?.getMobileNumber()

        if (currentUserPhoneNumber != null) {
            db.collection("nearby_drivers")
                .document(currentUserPhoneNumber)
                .collection("allDrivers")
                .whereEqualTo("rideStatus", "pending")
                .addSnapshotListener { snapshot, exception ->
                    if (exception != null) {
                        Toast.makeText(this, "Error fetching nearby drivers: ${exception.message}", Toast.LENGTH_SHORT).show()
                        return@addSnapshotListener
                    }

                    if (snapshot != null && !snapshot.isEmpty) {
                        val nearbyDrivers = mutableListOf<DocumentSnapshot>()

                        for (driver in snapshot.documents) {
                            val driverPhoneNumber = driver.getString("number")

                            if (driverPhoneNumber == currentUserPhoneNumber) {
                                nearbyDrivers.add(driver)
                            }
                        }

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
    }

    private fun showRideRequestDialog(fromLocation: String, toLocation: String, drivers: List<DocumentSnapshot>) {
        val userPhoneNumber = appSharedPreferences?.getMobileNumber()
        val userType = appSharedPreferences?.getUserType()

        if (userType == "Driver") {
            drivers.forEach { driver ->
                val driverPhoneNumber = driver.getString("number")
                val driverName = driver.getString("name") ?: "Unknown Driver"
                val rideRequestUserName = driver.getString("userName") ?: "Unknown Name"
                val rideRequestUserNumber = driver.getString("userPhoneNumber") ?: "Unknown Number"

                if (driverPhoneNumber == userPhoneNumber && driverPhoneNumber != null) {
                    val dialogBuilder = AlertDialog.Builder(this)

                    val dialog = dialogBuilder.setTitle("Ride Request")
                        .setMessage("User is requesting a ride from $fromLocation to $toLocation.\n\nDo you want to accept the ride?")
                        .setPositiveButton("Accept") { _, _ ->
                            handleRideAcceptance(driverPhoneNumber, rideRequestUserName, rideRequestUserNumber, fromLocation, toLocation)
                            removeRideRequest(driverPhoneNumber, rideRequestUserNumber, fromLocation, toLocation)
                        }
                        .setNegativeButton("Cancel") { _, _ ->
                            handleRideRejection(driverPhoneNumber)
                            removeRideRequest(driverPhoneNumber, rideRequestUserNumber, fromLocation, toLocation)
                        }
                        .create()

                    dialog.show()
                }
            }
        } else {
            Toast.makeText(this, "Only drivers can accept rides.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun handleRideRejection(driverId: String) {
        Toast.makeText(this, "Driver $driverId rejected the ride", Toast.LENGTH_SHORT).show()
    }

    private fun handleRideAcceptance(driverPhoneNumber: String, requestedRideUserName: String, requestedRideUserNumber: String, fromLocation: String, toLocation: String) {
        val currentUserPhoneNumber = appSharedPreferences?.getMobileNumber()

        if (currentUserPhoneNumber != null) {
            db.collection("drivers")
                .document(driverPhoneNumber)
                .get()
                .addOnSuccessListener { driverDocument ->
                    if (driverDocument.exists()) {
                        val driverName = driverDocument.getString("name") ?: "Unknown Driver"

                        val id = UUID.randomUUID().toString()
                        val rideRequestData = hashMapOf(
                            "id" to id,
                            "requestedRideUserNumber" to requestedRideUserNumber,
                            "requestedRideUserName" to requestedRideUserName,
                            "userLocation" to fromLocation,
                            "toLocation" to toLocation,
                            "rideStatus" to "accepted",
                            "rideAcceptedAt" to System.currentTimeMillis(),
                            "driverPhoneNumber" to driverPhoneNumber,
                            "driverName" to driverName,
                            "driverVehicleType" to driverDocument.getString("vehicleType"),
                            "driverLatitude" to driverDocument.getDouble("latitude"),
                            "driverLongitude" to driverDocument.getDouble("longitude")
                        )

                        db.collection("ride_requests")
                            .document(driverPhoneNumber)
                            .collection("rides")
                            .document(id)
                            .set(rideRequestData)
                            .addOnSuccessListener {
                                Toast.makeText(this, "Ride accepted successfully!", Toast.LENGTH_LONG).show()
                                updateDriverStatusToOnRide(driverPhoneNumber)
                                removeAllNearbyDriversRideRequests(requestedRideUserNumber)
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

    private fun getAllRideForCurrentDriver() {
        val currentUserPhoneNumber = appSharedPreferences?.getMobileNumber()

        if (currentUserPhoneNumber != null) {
            db.collection("ride_requests")
                .document(currentUserPhoneNumber)
                .collection("rides")
                .addSnapshotListener { snapshot, exception ->
                    if (exception != null) {
                        Toast.makeText(this, "Error fetching ride requests: ${exception.message}", Toast.LENGTH_SHORT).show()
                        return@addSnapshotListener
                    }

                    if (snapshot != null && !snapshot.isEmpty) {
                        rideRequestsList.clear()
                        for (document in snapshot.documents) {
                            val driverRide = document.toObject(DriverRides::class.java)
                            driverRide?.let { rideRequestsList.add(it) }
                        }

                        rideRequestAdapter.notifyDataSetChanged()
                    }
                }
        }
    }

    private fun removeAllNearbyDriversRideRequests(requestedRideUserNumber: String) {
        db.collection("nearby_drivers")
            .whereEqualTo("userPhoneNumber", requestedRideUserNumber)
            .whereEqualTo("rideStatus", "pending")
            .get()
            .addOnSuccessListener { snapshot ->
                if (!snapshot.isEmpty) {
                    for (document in snapshot.documents) {
                        db.collection("nearby_drivers")
                            .document(document.id)
                            .delete()
                            .addOnSuccessListener {
                                Toast.makeText(this, "Removed ride request for ${document.getString("number")}", Toast.LENGTH_SHORT).show()
                            }
                            .addOnFailureListener { e ->
                                Toast.makeText(this, "Failed to remove ride request: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                    }
                } else {
                    Toast.makeText(this, "No ride requests found for the requested user.", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error fetching nearby drivers: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

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

    private fun removeRideRequest(driverPhoneNumber: String, rideRequestUserNumber: String, fromLocation: String, toLocation: String) {
        db.collection("nearby_drivers")
            .document(driverPhoneNumber)
            .collection("allDrivers")
            .whereEqualTo("userPhoneNumber", rideRequestUserNumber)
            .whereEqualTo("fromLocation", fromLocation)
            .whereEqualTo("toLocation", toLocation)
            .get()
            .addOnSuccessListener { snapshot ->
                if (!snapshot.isEmpty) {
                    for (document in snapshot.documents) {
                        db.collection("nearby_drivers")
                            .document(driverPhoneNumber)
                            .collection("allDrivers")
                            .document(document.id)
                            .delete()
                            .addOnSuccessListener {
                                Toast.makeText(this, "Ride request removed successfully.", Toast.LENGTH_SHORT).show()
                            }
                            .addOnFailureListener { e ->
                                Toast.makeText(this, "Failed to remove ride request: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                    }
                } else {
                    Toast.makeText(this, "No matching ride request found.", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error fetching ride request: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    override fun onAcceptClick(rideRequestUserNumber: String) {}

    override fun onCancelClick(driverPhoneNumber: String, id: String) {
        appSharedPreferences?.getMobileNumber()?.let { currentUserPhoneNumber ->
            db.collection("ride_requests")
                .document(currentUserPhoneNumber)
                .collection("rides")
                .document(id)
                .delete()
                .addOnSuccessListener {
                    val rideToRemove = rideRequestsList.find { it.id == id }
                    rideToRemove?.let {
                        rideRequestsList.remove(it)
                    }
                    rideRequestAdapter.notifyDataSetChanged()
                    Toast.makeText(this, "Ride Cancelled", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Failed to cancel ride: ${e.message}", Toast.LENGTH_SHORT).show()
                }

            db.collection("drivers")
                .document(driverPhoneNumber)
                .update("status", "Active")
                .addOnSuccessListener {
                    Toast.makeText(this, "Driver status updated to 'Active'.", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Failed to update driver status: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }
}