package com.vinted.coper.example

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.vinted.coper.Coper
import com.vinted.coper.PermissionResult
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
        launch {
            val result = coper.request(permissionOne, permissionTwo)
            _permissionRequestResultEvent.postValue(result)
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
}
