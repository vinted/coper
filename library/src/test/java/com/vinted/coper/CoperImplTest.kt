package com.vinted.coper

import android.os.Build
import androidx.fragment.app.FragmentActivity
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.assertEquals


@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [Build.VERSION_CODES.O_MR1])
class CoperImplTest {

    private val activityController = Robolectric.buildActivity(FragmentActivity::class.java)
    lateinit var fixture: Coper

    @Before
    fun setup() {
        fixture = CoperImpl(activityController.setup().get().supportFragmentManager)
    }

    @Test
    fun request_responseIsNull() = runBlocking {
        val response = fixture.request("test")

        assertEquals(true, response.isSuccessful())
    }
}
