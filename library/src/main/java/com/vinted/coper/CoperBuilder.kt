package com.vinted.coper

import androidx.fragment.app.FragmentManager

class CoperBuilder {

    private var fragmentManager: FragmentManager? = null

    fun setFragmentManager(fragmentManager: FragmentManager): CoperBuilder {
        this.fragmentManager = fragmentManager
        return this
    }

    fun build(): Coper {
        val fragmentManager = fragmentManager
        require(fragmentManager != null) { "Coper cant be built without fragment manager" }
        return CoperImpl(fragmentManager)
    }
}
