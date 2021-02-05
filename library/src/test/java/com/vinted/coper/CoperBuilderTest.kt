package com.vinted.coper

import android.os.Build
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.assertEquals

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [Build.VERSION_CODES.O_MR1])
class CoperBuilderTest {

    private val activity = Robolectric.buildActivity(FragmentActivity::class.java).setup().get()
    lateinit var fragmentManager: FragmentManager
    lateinit var fixture: CoperBuilder

    @Before
    fun setup() {
        fragmentManager = activity.supportFragmentManager
        fixture = CoperBuilder()
    }

    @Test
    fun build_fragmentManagerPassed_buildSuccessful() {
        fixture.setFragmentManager(fragmentManager)

        val result = fixture.build()

        assertEquals(CoperImpl::class.toString(), result::class.toString())
    }

    @Test
    fun build_fragmentActivityPassed_buildSuccessful() {
        fixture.setFragmentActivity(activity)

        val result = fixture.build()

        assertEquals(CoperImpl::class.toString(), result::class.toString())
    }

    @Test(expected = IllegalArgumentException::class)
    fun build_noActivityAndFragmentPassed_buildThrowError() {
        fixture.build()
    }
}
