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

    private var resultFragmentCreationTimeout: Long? = null

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
     * Set timeout for result fragment preparation.
     * Default -> 1s
     */
    fun setResultFragmentPreparationTimeout(timeout: Long): CoperBuilder {
        this.resultFragmentCreationTimeout = timeout
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
            fragmentAwaitingTimeout = resultFragmentCreationTimeout
        )
    }

    companion object {
        internal const val DEFAULT_FRAGMENT_PREPARATION_TIMEOUT = 1000L
    }
}
