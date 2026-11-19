package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.model.DesyncMode
import com.example.model.IspPreset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("GoodbyeDPI TR", appName)
    }

    @Test
    fun `turkey standard preset generates expected goodbyedpi command`() {
        val config = IspPreset.TURKEY_STANDARD.defaultConfig
        val cmd = config.toCommandLineString()
        assertEquals(DesyncMode.MODE_5, config.desyncMode)
        assertTrue(cmd.contains("-5"))
        assertTrue(cmd.contains("--dns-addr 77.88.8.8 --dns-port 1253"))
    }
}
