package com.test.weareconnected.view.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.test.weareconnected.DriverRideClickListener
import com.test.weareconnected.databinding.ItemRideRequestBinding
import com.test.weareconnected.models.DriverRides

class RideRequestAdapter(
    private val rideRequests: List<DriverRides>,val driverRideClickListener: DriverRideClickListener
) : RecyclerView.Adapter<RideRequestAdapter.RideRequestViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RideRequestViewHolder {
        val binding = ItemRideRequestBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return RideRequestViewHolder(binding)
    }

    override fun onBindViewHolder(holder: RideRequestViewHolder, position: Int) {
        val rideRequest = rideRequests[position]
        holder.bind(rideRequest)
    }

    override fun getItemCount(): Int = rideRequests.size

    inner class RideRequestViewHolder(private val binding: ItemRideRequestBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(rideRequest: DriverRides) {
            // Binding data from DriverRides object to the views
            binding.userNameTextView.text = rideRequest.requestedRideUserName
            binding.userPhoneNumberTextView.text = rideRequest.requestedRideUserNumber
            binding.fromLocationTextView.text = rideRequest.userLocation
            binding.toLocationTextView.text = rideRequest.toLocation

            // Optionally, you can handle the buttons here
            // Example: Accept button click
            binding.acceptButton.setOnClickListener {
                // Handle accept action (e.g., update Firestore or UI)
                // You can pass the rideRequest to a callback or directly handle here
            }

            // Example: Cancel button click
            binding.cancelButton.setOnClickListener {
               driverRideClickListener.onCancelClick(rideRequest.driverPhoneNumber,rideRequest.id)
            }
        }
    }
}
