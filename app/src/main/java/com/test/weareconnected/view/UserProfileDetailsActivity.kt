package com.test.weareconnected.view

import android.content.Intent
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

            firestoreDB.collection(collection)
                .document(number)
                .get()
                .addOnSuccessListener { document ->
                    binding.progressBar.visibility = View.GONE

                    if (document.exists()) {
                        val name = document.getString("name")
                        val mobile = document.getString("number")
                        val age = document.getLong("age")?.toString()

                        binding.nameInput.setText(name)
                        binding.mobileInput.setText(mobile)
                        binding.age.setText(age)
                    } else {
                        binding.nameInput.setText("")
                        binding.mobileInput.setText("")
                        binding.age.setText("")
                    }
                }
                .addOnFailureListener { exception ->
                    binding.progressBar.visibility = View.GONE

                }
        }

        binding.logoutBtn.setOnClickListener {
            appSharedPreferences?.clearAllData()
            val intent = Intent(this,LoginActivity::class.java)
            startActivity(intent)
            finishAffinity()
        }

        binding.updateProfile.setOnClickListener {
            val name = binding.nameInput.text.toString().trim()
            val mobile = binding.mobileInput.text.toString().trim()
            val age = binding.age.text.toString().trim()

            if (name.isNotEmpty() && mobile.isNotEmpty() && age.isNotEmpty()) {
                binding.progressBar.visibility = View.VISIBLE

                // Update the user data in Firestore
                firestoreDB.collection("users")
                    .document(number ?: "")
                    .update(
                        "name", name,
                        "number", mobile,
                        "age", age.toInt()
                    )
                    .addOnSuccessListener {
                        binding.progressBar.visibility = View.GONE
                        finish()
                    }
                    .addOnFailureListener { e ->
                        binding.progressBar.visibility = View.GONE
                    }
            } else {
            }
        }
    }
}
