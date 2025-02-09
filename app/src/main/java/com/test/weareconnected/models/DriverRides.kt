package com.test.weareconnected.models

data class DriverRides(
    val id:String = "",
    val requestedRideUserNumber: String = "",
    val requestedRideUserName: String = "",
    val userLocation: String = "",
    val toLocation: String = "",
    val rideStatus: String = "",
    val rideAcceptedAt: Long = 0L,
    val driverPhoneNumber: String = "",
    val driverName: String = "",
    val driverVehicleType: String = "",
    val driverLatitude: Double = 0.0,
    val driverLongitude: Double = 0.0
)
