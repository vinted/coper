package com.vinted.coper

import androidx.fragment.app.FragmentManager

/**
 * [Coper] instance builder.
 * Use case:
 * ```
 * val coper = CoperBuilder()
 *      .setFragmentManager(fragmentManager)
 *      .build()
 * ```
 * If [fragmentManager] will be null on [build], then it will throw [IllegalArgumentException].
 */
class CoperBuilder {

    private var fragmentManager: FragmentManager? = null

    /**
     * Set fragment manager for permission handling.
     */
    fun setFragmentManager(fragmentManager: FragmentManager): CoperBuilder {
        this.fragmentManager = fragmentManager
        return this
    }

    /**
     * Build [Coper].
     * If [fragmentManager] will be null on [build], then it will throw [IllegalArgumentException].
     * @return [Coper]
     */
    fun build(): Coper {
        val fragmentManager = fragmentManager
        require(fragmentManager != null) { "Coper cant be built without fragment manager" }
        return CoperImpl(fragmentManager)
    }
}
