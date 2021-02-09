package com.vinted.coper

import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager

/**
 * [Coper] instance builder.
 * Use case:
 * ```
 * val coper = CoperBuilder()
 *      .setFragmentActivity(activity)
 *      .build()
 * ```
 * If [fragmentManager] will be null on [build], then it will throw [IllegalArgumentException].
 */
class CoperBuilder {

    private var activity: FragmentActivity? = null

    private var fragmentManager: FragmentManager? = null

    /**
     * Set fragment activity for permission handling.
     */
    fun setFragmentActivity(fragmentActivity: FragmentActivity): CoperBuilder {
        this.activity = fragmentActivity
        return this
    }

    /**
     * Set fragment manager for permission handling.
     */
    @Deprecated(
        message = "Please use setFragmentActivity as it will provide safer usage",
        replaceWith = ReplaceWith("setFragmentActivity(activity)")
    )
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
        val fragmentActivity = activity
        val fragmentManager = fragmentManager ?: activity?.supportFragmentManager
        require(fragmentManager != null) { "Coper cant be built without fragment manager" }
        return CoperImpl(
            fragmentManager = fragmentManager,
            lifecycle = fragmentActivity?.lifecycle,
        )
    }
}
