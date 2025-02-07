package com.test.weareconnected.models

data class Driver(
    val driverId: String,
    val name: String,
    val number: String,
    val vehicleType: String,
    val age: Int,
    val password: String,
    val userType: String,
    val status: String,
    val latitude: Double?,
    val longitude: Double?
)
