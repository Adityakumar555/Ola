package com.test.weareconnected.notification

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.google.firebase.firestore.FirebaseFirestore
import com.test.weareconnected.R
import com.test.weareconnected.utils.AppSharedPreferences

class RideNotificationService : Service() {

    private val db = FirebaseFirestore.getInstance()
    private val appSharedPreferences by lazy { AppSharedPreferences.getInstance(this) }
    private val CHANNEL_ID = "RIDE_NOTIFICATION_CHANNEL"

    override fun onCreate() {
        super.onCreate()
        listenForRideAcceptance()
        fetchNearbyRides()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    // Fetch nearby rides and notify the driver
    private fun fetchNearbyRides() {
        val currentUserPhoneNumber = appSharedPreferences?.getMobileNumber()
        if (currentUserPhoneNumber != null) {
            db.collection("nearby_drivers")
                .whereEqualTo("number", currentUserPhoneNumber)
                .whereEqualTo("rideStatus", "pending")
                .addSnapshotListener { snapshot, _ ->
                    snapshot?.documents?.firstOrNull()?.let { user ->
                        val rideRequestUserName = user.getString("userName") ?: "Unknown Name"
                        val fromLocation = user.getString("fromLocation") ?: "Unknown Location"
                        val toLocation = user.getString("toLocation") ?: "Unknown Location"

                        showRideNotification(
                            "New Ride Request",
                            "User $rideRequestUserName has requested a ride.",
                            "A new ride request from: $fromLocation to: $toLocation."
                        )
                    }
                }
        }
    }

    // Listen for ride acceptance and notify the user
    private fun listenForRideAcceptance() {
        val userPhoneNumber = appSharedPreferences?.getMobileNumber() ?: ""

        db.collection("ride_requests")
            .whereEqualTo("requestedRideUserNumber", userPhoneNumber)
            .addSnapshotListener { rideRequestSnapshot, _ ->
                rideRequestSnapshot?.documents?.firstOrNull()?.let { rideRequestDoc ->
                    val rideStatus = rideRequestDoc.getString("rideStatus")
                    val driverName = rideRequestDoc.getString("driverName") ?: "Unknown Driver"
                    val fromLocation = rideRequestDoc.getString("userLocation") ?: "Unknown Location"
                    val toLocation = rideRequestDoc.getString("toLocation") ?: "Unknown Location"

                    if (rideStatus == "accepted") {
                        showRideNotification(
                            "Ride Accepted",
                            "Your ride has been accepted by $driverName.",
                            "Your ride from: $fromLocation to: $toLocation has been accepted."
                        )
                    }
                }
            }
    }

    // Show a notification
    private fun showRideNotification(title: String, message: String, bigText: String) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val notificationChannel = NotificationChannel(CHANNEL_ID,"RideApp",NotificationManager.IMPORTANCE_HIGH)
        notificationManager.createNotificationChannel(notificationChannel)


        val bigTextStyle = NotificationCompat.BigTextStyle().bigText(bigText)
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(bigTextStyle)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()



        notificationManager.notify(10, notification)
    }


}
