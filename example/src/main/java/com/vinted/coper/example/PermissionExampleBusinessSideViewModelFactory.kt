package com.vinted.coper.example

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.vinted.coper.Coper

class PermissionExampleBusinessSideViewModelFactory(
    private val coper: Coper
): ViewModelProvider.Factory {
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        return if (modelClass.isAssignableFrom(PermissionExampleInBusinessSideViewModel::class.java)) {
            PermissionExampleInBusinessSideViewModel(coper) as T
        } else {
            throw IllegalArgumentException("Not Found")
        }
    }
}
