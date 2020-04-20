package com.vinted.coper.example

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.vinted.coper.Coper
import com.vinted.coper.PermissionResult
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch

class PermissionExampleInBusinessSideViewModel(
    private val coper: Coper
) : ViewModel(), CoroutineScope by MainScope() {

    private val _permissionRequestResultEvent = MutableLiveData<PermissionResult>()
    val permissionRequestResultEvent: LiveData<PermissionResult> = _permissionRequestResultEvent

    fun onOnePermissionClicked(permission: String) {
        launch {
            val result = coper.request(permission)
            _permissionRequestResultEvent.postValue(result)
        }
    }

    fun onTwoPermissionsClickedWithOneRequest(permissionOne: String, permissionTwo: String) {
        launch(CoroutineExceptionHandler { _, exception ->
            Log.e(TAG, "Permission request not successful: $exception")
        }) {
            coper.withPermissions(permissionOne, permissionTwo) {
                _permissionRequestResultEvent.postValue(it)
            }
        }
    }

    fun onTwoPermissionsClickedWithTwoRequests(permissionOne: String, permissionTwo: String) {
        launch {
            val firstPermissionResult = coper.request(permissionOne)
            val secondPermissionResult = coper.request(permissionTwo)
            _permissionRequestResultEvent.postValue(firstPermissionResult)
            _permissionRequestResultEvent.postValue(secondPermissionResult)
        }
    }

    companion object {
        private const val TAG = "PermissionViewModel"
    }
}
