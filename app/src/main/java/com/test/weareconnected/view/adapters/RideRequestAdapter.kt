package com.test.weareconnected.view.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.test.weareconnected.DriverRideClickListener
import com.test.weareconnected.databinding.ItemRideRequestBinding
import com.test.weareconnected.models.DriverRides

class RideRequestAdapter(
    private val rideRequests: List<DriverRides>, private val driverRideClickListener: DriverRideClickListener
) : RecyclerView.Adapter<RideRequestAdapter.RideRequestViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RideRequestViewHolder {
        val binding = ItemRideRequestBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return RideRequestViewHolder(binding)
    }

    override fun onBindViewHolder(holder: RideRequestViewHolder, position: Int) {
        val rideRequest = rideRequests[position]
        holder.binding.userNameTextView.text = rideRequest.requestedRideUserName
        holder.binding.userPhoneNumberTextView.text = rideRequest.requestedRideUserNumber
        holder.binding.fromLocationTextView.text = rideRequest.userLocation
        holder.binding.toLocationTextView.text = rideRequest.toLocation

        holder.binding.acceptButton.setOnClickListener {
        }

        //  Cancel button click
        holder.binding.cancelButton.setOnClickListener {
            driverRideClickListener.onCancelClick(rideRequest.driverPhoneNumber,rideRequest.id)
        }
    }

    override fun getItemCount(): Int = rideRequests.size

    class RideRequestViewHolder(val binding: ItemRideRequestBinding) : RecyclerView.ViewHolder(binding.root) {}
}
