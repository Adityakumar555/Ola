package com.test.weareconnected.notification

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.test.weareconnected.R
import com.test.weareconnected.utils.AppSharedPreferences

class RideNotificationService : LifecycleService() {

    private val db = FirebaseFirestore.getInstance()
    private val appSharedPreferences by lazy { AppSharedPreferences.getInstance(this) }

    override fun onCreate() {
        super.onCreate()
        listenForRideAcceptance()
        fetchNearbyRides()
    }

    // Fetch nearby rides and notify the driver
    private fun fetchNearbyRides() {
        val currentUserPhoneNumber = appSharedPreferences?.getMobileNumber()

        // Set up a real-time listener on the 'nearby_drivers'
        db.collection("nearby_drivers")
            .whereEqualTo("rideStatus", "pending")
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

                    // If matching drivers were found, show the ride request notification to the driver
                    if (nearbyDrivers.isNotEmpty()) {
                        // Fetch the user data who requested the ride
                        val user = snapshot.documents.firstOrNull()
                        val rideRequestUserName = user?.getString("userName") ?: "Unknown Name"
                        val fromLocation = user?.getString("fromLocation") ?: "Unknown Location"
                        val toLocation = user?.getString("toLocation") ?: "Unknown Location"

                        // Show a local notification to the driver about the new ride request
                        showRideNotification(
                            title = "New Ride Request",
                            message = "User $rideRequestUserName has requested a ride.",
                            bigText = "A new ride request has been made by $rideRequestUserName. From: $fromLocation to: $toLocation.",
                            fromLocation = fromLocation,
                            toLocation = toLocation
                        )
                    }
                }
            }
    }

    // This function is to notify the driver about the ride request or ride acceptance
    private fun showRideNotification(
        title: String,
        message: String,
        bigText: String,
        fromLocation: String,
        toLocation: String
    ) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "RIDE_NOTIFICATION_CHANNEL"

        // Create notification channel for Android 8 and above
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Ride Notifications",
                NotificationManager.IMPORTANCE_HIGH
            )
            notificationManager.createNotificationChannel(channel)
        }

        val bigTextStyle = NotificationCompat.BigTextStyle()
            .bigText(bigText)

        val notification: Notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(bigTextStyle)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)

            .build()

        // Show notification
        notificationManager.notify(2, notification)
    }

    // Listen for ride acceptance and show a notification if accepted
    private fun listenForRideAcceptance() {
        val userPhoneNumber = appSharedPreferences?.getMobileNumber() ?: ""

        db.collection("ride_requests")
            .whereEqualTo("requestedRideUserNumber", userPhoneNumber)
            .addSnapshotListener { rideRequestSnapshot, e ->
                if (e != null) {
                    return@addSnapshotListener
                }

                rideRequestSnapshot?.documents?.firstOrNull()?.let { rideRequestDoc ->
                    val rideStatus = rideRequestDoc.getString("rideStatus")
                    val driverName = rideRequestDoc.getString("driverName") ?: "Unknown Driver"
                    val fromLocation = rideRequestDoc.getString("userLocation") ?: "Unknown Location"
                    val toLocation = rideRequestDoc.getString("toLocation") ?: "Unknown Location"

                    if (rideStatus == "accepted") {
                        // Show the notification for ride acceptance
                        showRideNotification(
                            title = "Ride Accepted",
                            message = "Your ride has been accepted by $driverName.",
                            bigText = "Your ride has been accepted by $driverName. From: $fromLocation to: $toLocation. Please be ready.",
                            fromLocation = fromLocation,
                            toLocation = toLocation
                        )
                    }
                }
            }
    }

}
