package com.test.weareconnected.models

data class LocationData(
    val fullAddress:String,
    val street: String,
    val block: String,
    val locality: String,
    val state: String,
    val subState: String,
    val postalCode: String,
    val country: String
)
