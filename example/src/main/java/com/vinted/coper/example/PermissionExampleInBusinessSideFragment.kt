package com.vinted.coper.example

import android.Manifest
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import by.kirich1409.viewbindingdelegate.viewBinding
import com.vinted.coper.CoperBuilder
import com.vinted.coper.PermissionResult
import com.vinted.coper.example.databinding.FragmentPermissionExampleInBusinessSideBinding

class PermissionExampleInBusinessSideFragment : Fragment() {

    private val viewBinding: FragmentPermissionExampleInBusinessSideBinding by viewBinding(
        FragmentPermissionExampleInBusinessSideBinding::bind
    )

    private lateinit var viewModel: PermissionExampleInBusinessSideViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val coper = CoperBuilder()
            .setFragmentActivity(requireActivity())
            .build()
        viewModel = ViewModelProvider(
            this,
            PermissionExampleBusinessSideViewModelFactory(coper = coper)
        ).get(PermissionExampleInBusinessSideViewModel::class.java)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(
            R.layout.fragment_permission_example_in_business_side,
            container,
            false
        )
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel.permissionRequestResultEvent.observe(viewLifecycleOwner, {
            onPermissionResult(it)
        })

        viewBinding.permissionExampleInBusinessRequestOnePermission.setOnClickListener {
            viewModel.onOnePermissionClicked(Manifest.permission.ACCESS_FINE_LOCATION)
        }

        viewBinding.permissionExampleInBusinessRequestTwoPermissionOneRequest.setOnClickListener {
            viewModel.onTwoPermissionsClickedWithOneRequest(
                permissionOne = Manifest.permission.ACCESS_FINE_LOCATION,
                permissionTwo = Manifest.permission.CAMERA
            )
        }
        viewBinding.permissionExampleInBusinessRequestTwoPermissionTwoRequests.setOnClickListener {
            viewModel.onTwoPermissionsClickedWithTwoRequests(
                permissionOne = Manifest.permission.ACCESS_FINE_LOCATION,
                permissionTwo = Manifest.permission.CAMERA
            )
        }
    }

    private fun onPermissionResult(permissionResult: PermissionResult) {
        when (permissionResult) {
            is PermissionResult.Granted -> {
                showToast("${permissionResult.grantedPermissions} permissions granted")
            }
            is PermissionResult.Denied -> {
                handleDeniedPermission(permissionResult)
            }
        }
    }

    private fun handleDeniedPermission(permissionResult: PermissionResult.Denied) {
        if (permissionResult.isRationale()) {
            showToast("${permissionResult.getDeniedRationale()} permission denied and needs rationale")
        } else {
            showToast("${permissionResult.getDeniedPermanently()} permission denied permanently")
        }
    }

    private fun showToast(text: String) {
        Toast.makeText(
            requireContext(),
            text,
            Toast.LENGTH_SHORT
        ).show()
    }
}
