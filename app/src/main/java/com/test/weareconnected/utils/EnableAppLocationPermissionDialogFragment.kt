package com.test.weareconnected.utils

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import com.test.weareconnected.databinding.FragmentEnableAppLocationPermissionDialogBinding

class EnableAppLocationPermissionDialogFragment : DialogFragment() {

    private lateinit var binding: FragmentEnableAppLocationPermissionDialogBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentEnableAppLocationPermissionDialogBinding.inflate(layoutInflater, container, false)

        /*// Determine the caller based on the tag
        updateTextColor(tag)*/

        binding.goToSetting.setOnClickListener {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
            val uri = Uri.fromParts("package", requireContext().packageName, null)
            intent.data = uri
            startActivity(intent)
            dismiss()
        }

        return binding.root
    }


    override fun onStart() {
        super.onStart()
        val dialog = dialog
        dialog?.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )

    }
}
