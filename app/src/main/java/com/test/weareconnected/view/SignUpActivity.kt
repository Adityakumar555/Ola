package com.test.weareconnected.view

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.firebase.firestore.FirebaseFirestore
import com.test.weareconnected.R
import com.test.weareconnected.databinding.ActivitySignUpBinding
import com.test.weareconnected.models.Driver
import com.test.weareconnected.models.User
import com.test.weareconnected.utils.AppSharedPreferences
import com.test.weareconnected.utils.EnableAppLocationPermissionDialogFragment
import com.test.weareconnected.utils.MyHelper
import java.util.UUID

class SignUpActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySignUpBinding
    private val db = FirebaseFirestore.getInstance()  // Firestore instance
    private val appSharedPreferences by lazy { AppSharedPreferences.getInstance(this) }
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private val myHelper by lazy { MyHelper(this) }
    private lateinit var gpsLauncher: ActivityResultLauncher<IntentSenderRequest>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Inflate the layout using ViewBinding
        binding = ActivitySignUpBinding.inflate(layoutInflater)
        setContentView(binding.root)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        // Setup vehicle type spinner
        val vehicleTypeSpinner = findViewById<Spinner>(R.id.vehicle_type_spinner)
        val vehicleTypes = arrayOf("Car", "Auto", "Bike")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, vehicleTypes)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        vehicleTypeSpinner.adapter = adapter

        // Register GPS setting prompt launcher
        gpsLauncher = registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                // If the user enables GPS, fetch the location
            } else {
                // Inform the user if GPS is required
                Toast.makeText(this, "GPS is required to proceed.", Toast.LENGTH_SHORT).show()
            }
        }

        // Listen to changes in the radio group selection
        binding.userTypeGroup.setOnCheckedChangeListener { _, checkedId ->
            // Check the user type
            val userType = if (checkedId == binding.userRadioButton.id) "User" else "Driver"

            // Set visibility of vehicleType based on user type
            if (userType == "Driver") {
                binding.vehicleType.visibility = View.VISIBLE
            } else {
                binding.vehicleType.visibility = View.GONE
            }
        }

        // Set up the Sign Up Button Click Listener
        binding.registerButton.setOnClickListener {
            // Get the inputs from the EditText fields
            val name = binding.yourName.text.toString()
            val number = binding.yourNumber.text.toString()
            val age = binding.yourAge.text.toString()
            val password = binding.yourPassword.text.toString()

            val userTypeId = binding.userTypeGroup.checkedRadioButtonId
            val userType = if (userTypeId == binding.userRadioButton.id) "User" else "Driver"

            // Validate the inputs
            if (name.isEmpty()) {
                Toast.makeText(this, "Name cannot be empty", Toast.LENGTH_SHORT).show()
            } else if (number.isEmpty() || number.length != 10) {
                Toast.makeText(this, "Please enter a valid 10-digit phone number", Toast.LENGTH_SHORT).show()
            } else if (age.isEmpty() || age.toIntOrNull() == null || age.toInt() <= 0) {
                Toast.makeText(this, "Please enter a valid age", Toast.LENGTH_SHORT).show()
            } else if (password.isEmpty() || password.length < 6) {
                Toast.makeText(this, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show()
            } else {
                // Handle location fetching
                if (myHelper.isLocationEnable()) {
                    if (myHelper.checkLocationPermission()) {
                        // Get the current location of the user
                        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                            if (location != null) {
                                val latitude = location.latitude
                                val longitude = location.longitude

                                // Generate a unique ID for the user or driver using UUID
                                val userId = UUID.randomUUID().toString()

                                // If user type is "Driver", save location details
                                if (userType == "Driver") {
                                    val vehicleType = binding.vehicleTypeSpinner.selectedItem.toString()

                                    // Create a Driver object
                                    val driverData = Driver(
                                        driverId = userId,
                                        name = name,
                                        number = number,
                                        vehicleType = vehicleType,
                                        age = age.toInt(),
                                        password = password,
                                        userType = userType,
                                        status = "Active",
                                        latitude = latitude,
                                        longitude = longitude
                                    )

                                    // Save driver data in Firestore
                                    db.collection("drivers")
                                        .document(number)
                                        .set(driverData)
                                        .addOnSuccessListener {
                                            Toast.makeText(this, "Driver Registration Successful", Toast.LENGTH_SHORT).show()

                                            // Save driverId to SharedPreferences
                                            appSharedPreferences?.saveDriverId(userId)

                                            // Save user information to SharedPreferences
                                            appSharedPreferences?.saveMobileNumber(number)
                                            appSharedPreferences?.saveUserType(userType)
                                            appSharedPreferences?.saveUserName(name)

                                            // Navigate to the DriverActivity Activity
                                            val intent = Intent(this, DriverActivity::class.java)
                                            startActivity(intent)
                                            finish()
                                        }
                                        .addOnFailureListener { e ->
                                            Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                                        }

                                } else {
                                    // Create a User object
                                    val userData = User(
                                        name = name,
                                        id = userId,
                                        number = number,
                                        age = age.toInt(),
                                        password = password,
                                        userType = userType
                                    )

                                    // Save user data in Firestore
                                    db.collection("users").document(number)
                                        .set(userData)
                                        .addOnSuccessListener {
                                            Toast.makeText(this, "User Registration Successful", Toast.LENGTH_SHORT).show()

                                            // Save user information to SharedPreferences
                                            appSharedPreferences?.saveMobileNumber(number)
                                            appSharedPreferences?.saveUserType(userType)
                                            appSharedPreferences?.saveUserName(name)

                                            // Navigate to the Main Activity
                                            val intent = Intent(this, MainActivity::class.java)
                                            startActivity(intent)
                                            finish()
                                        }
                                        .addOnFailureListener { e ->
                                            Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                                        }
                                }
                            } else {
                                Toast.makeText(this, "Location not found. Please try again.", Toast.LENGTH_SHORT).show()
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

        // Navigate to the Login screen when Sign In text is clicked
        binding.signinText.setOnClickListener {
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
            finish()
        }
    }

    // Handle location permission result
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 10 && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            // Permission granted, fetch location
        } else {
            if (!shouldShowRequestPermissionRationale(permissions[0])) {
                // If permission is denied permanently, show a dialog to guide the user to settings
                val progressDialog = EnableAppLocationPermissionDialogFragment()
                progressDialog.show(supportFragmentManager, "requestLocationPermission")
            }
        }
    }
}
