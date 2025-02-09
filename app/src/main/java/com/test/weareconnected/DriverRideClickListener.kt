package com.test.weareconnected

interface DriverRideClickListener {
    fun onAcceptClick(rideRequestUserNumber:String)
    fun onCancelClick(driverPhoneNumber: String,id: String)
}