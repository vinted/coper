package com.vinted.coper.example

import android.Manifest
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import by.kirich1409.viewbindingdelegate.viewBinding
import com.vinted.coper.CoperBuilder
import com.vinted.coper.PermissionResult
import com.vinted.coper.example.databinding.FragmentPermissionExampleInUiBinding

class PermissionExampleInUIFragment : Fragment() {

    private val viewBinding: FragmentPermissionExampleInUiBinding by viewBinding(
        FragmentPermissionExampleInUiBinding::bind
    )

    private val coper by lazy {
        CoperBuilder()
            .setFragmentActivity(requireActivity())
            .build()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_permission_example_in_ui, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewBinding.permissionExampleInUiRequestOnePermission.setOnClickListener {
            onOnePermissionClicked()
        }

        viewBinding.permissionExampleInUiRequestTwoPermissionOneRequest.setOnClickListener {
            onTwoPermissionsClickedWithOneRequest()
        }
        viewBinding.permissionExampleInUiRequestTwoPermissionTwoRequests.setOnClickListener {
            onTwoPermissionsClickedWithTwoRequests()
        }
    }

    private fun onOnePermissionClicked() {
        viewLifecycleOwner.lifecycleScope.launchWhenCreated {
            val result = coper.request(Manifest.permission.ACCESS_FINE_LOCATION)
            onPermissionResult(result)
        }
    }

    private fun onTwoPermissionsClickedWithOneRequest() {
        viewLifecycleOwner.lifecycleScope.launchWhenCreated {
            coper.request(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.CAMERA
            ).onGranted { grantedResult ->
                handleGrantedPermission(grantedResult)
            }.onDenied { deniedResult ->
                handleDeniedPermission(deniedResult)
            }
        }
    }

    private fun onTwoPermissionsClickedWithTwoRequests() {
        viewLifecycleOwner.lifecycleScope.launchWhenCreated {
            val firstPermissionResult = coper.request(Manifest.permission.ACCESS_FINE_LOCATION)
            val secondPermissionResult = coper.request(Manifest.permission.CAMERA)
            onPermissionResult(firstPermissionResult)
            onPermissionResult(secondPermissionResult)
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

    private fun handleGrantedPermission(permissionResult: PermissionResult.Granted) {
        showToast("${permissionResult.grantedPermissions} permissions granted")
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
