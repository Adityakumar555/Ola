package com.test.weareconnected.view

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.firestore.FirebaseFirestore
import com.test.weareconnected.databinding.ActivityLoginBinding
import com.test.weareconnected.notification.RideNotificationService
import com.test.weareconnected.utils.AppSharedPreferences

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private val db = FirebaseFirestore.getInstance()
    private val appSharedPreferences by lazy { AppSharedPreferences.getInstance(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val serviceIntent = Intent(this, RideNotificationService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent)
        } else {
            startService(serviceIntent)
        }

        // Check if the user has already logged in
        if (appSharedPreferences?.isMainActivityVisited() == true) {
            navigateToNextPage()
        }

        // Set up the Login Button Click Listener
        binding.loginButton.setOnClickListener {
            val userNumber = binding.number.text.toString()
            val password = binding.password.text.toString()

            val userTypeId = binding.userTypeGroup.checkedRadioButtonId
            val selectedUserType =
                if (userTypeId == binding.userRadioButton.id) "User" else "Driver"

            if (userNumber.isEmpty()) {
                Toast.makeText(this, "Username cannot be empty", Toast.LENGTH_SHORT).show()
            } else if (password.isEmpty()) {
                Toast.makeText(this, "Password cannot be empty", Toast.LENGTH_SHORT).show()
            } else {

                val collection = if (selectedUserType == "User") "users" else "drivers"

                db.collection(collection)
                    .document(userNumber)
                    .get()
                    .addOnSuccessListener { document ->
                        if (document.exists()) {
                            val storedPassword = document.getString("password")
                            val userTypeInDB = document.getString("userType")
                            val userName = document.getString("name")

                            if (storedPassword == password) {
                                if (userTypeInDB == selectedUserType) {
                                    // Successful login
                                    Toast.makeText(
                                        this,
                                        "Login successful as $selectedUserType",
                                        Toast.LENGTH_SHORT
                                    ).show()

                                    // Save user info to SharedPreferences
                                    appSharedPreferences?.saveMobileNumber(userNumber)
                                    appSharedPreferences?.saveUserType(selectedUserType)
                                    if (userName != null) {
                                        appSharedPreferences?.saveUserName(userName)
                                    }

                                    // If driver, save the driver's ID
                                    if (selectedUserType == "Driver") {
                                        val driverId = document.getString("driverId")
                                        if (driverId != null) {
                                            appSharedPreferences?.saveDriverId(driverId)
                                        }
                                    }

                                    navigateToNextPage()
                                } else {
                                    Toast.makeText(this, "User type mismatch", Toast.LENGTH_SHORT)
                                        .show()
                                }
                            } else {
                                Toast.makeText(this, "Invalid password", Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            Toast.makeText(
                                this,
                                "User does not exist. Please register.",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                    .addOnFailureListener { exception ->
                        Toast.makeText(this, "Error: ${exception.message}", Toast.LENGTH_SHORT)
                            .show()
                    }
            }
        }


    // navigate to the sign-up activity
    binding.signupText.setOnClickListener{
        val intent = Intent(this, SignUpActivity::class.java)
        startActivity(intent)
        finish()
    }

}

// Navigate to the main activity
private fun navigateToNextPage() {
    val userType = appSharedPreferences?.getUserType()

    if (userType == "Driver") {
        val intent = Intent(this, DriverActivity::class.java) // Navigate to DriverActivity
        startActivity(intent)
    } else {
        val intent = Intent(this, UserActivity::class.java) // Navigate to MainActivity
        startActivity(intent)
    }
    finish()
}
}
