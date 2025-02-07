package com.test.weareconnected.view

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.firestore.FirebaseFirestore
import com.test.weareconnected.databinding.ActivityUserProfileDetailsBinding
import com.test.weareconnected.utils.AppSharedPreferences
import com.test.weareconnected.utils.PrefKeys

class UserProfileDetailsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityUserProfileDetailsBinding
    private val firestoreDB = FirebaseFirestore.getInstance()
    private val appSharedPreferences by lazy { AppSharedPreferences.getInstance(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityUserProfileDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val number = appSharedPreferences?.getMobileNumber()
        val selectedUserType = appSharedPreferences?.getUserType()

        binding.backButton.setOnClickListener {
            finish()
        }


        if (number != null) {
            // Show the progress bar while fetching data
            binding.progressBar.visibility = View.VISIBLE

            val collection = if (selectedUserType == "User") "users" else "drivers"

            // Fetch user data from Firestore using the mobile number as the document ID
            firestoreDB.collection(collection)
                .document(number)
                .get()
                .addOnSuccessListener { document ->
                    // Hide the progress bar after data is fetched
                    binding.progressBar.visibility = View.GONE

                    if (document.exists()) {
                        // Get the user data from Firestore
                        val name = document.getString("name")
                        val mobile = document.getString("number")
                        val age = document.getLong("age")?.toString()

                        // Set the values to the corresponding views
                        binding.nameInput.setText(name)
                        binding.mobileInput.setText(mobile)
                        binding.age.setText(age)
                    } else {
                        // Handle case where the document doesn't exist
                        binding.nameInput.setText("")
                        binding.mobileInput.setText("")
                        binding.age.setText("")
                    }
                }
                .addOnFailureListener { exception ->
                    // Hide the progress bar if there is an error
                    binding.progressBar.visibility = View.GONE
                    // Handle any errors that occurred during the query
                    // You can show a Toast message for error handling
                }
        }

        binding.updateProfile.setOnClickListener {
            val name = binding.nameInput.text.toString().trim()
            val mobile = binding.mobileInput.text.toString().trim()
            val age = binding.age.text.toString().trim()

            // Validate the inputs before updating
            if (name.isNotEmpty() && mobile.isNotEmpty() && age.isNotEmpty()) {
                // Show the progress bar while updating the data
                binding.progressBar.visibility = View.VISIBLE

                // Update the user data in Firestore
                firestoreDB.collection("users")
                    .document(number ?: "")
                    .update(
                        "name", name,
                        "number", mobile,
                        "age", age.toInt() // Assuming the age is a number
                    )
                    .addOnSuccessListener {
                        // Hide the progress bar after data is updated
                        binding.progressBar.visibility = View.GONE
                        // Show success message or navigate back
                        finish()
                    }
                    .addOnFailureListener { e ->
                        // Hide the progress bar in case of failure
                        binding.progressBar.visibility = View.GONE
                        // Handle failure during update
                    }
            } else {
                // Show validation error message
            }
        }
    }
}
