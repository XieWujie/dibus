package com.example.di

import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.runner.AndroidJUnit4
import com.xie.di.BUS_PREFIX
import com.xie.di.Bus
import com.xie.di.BusFetcher

import org.junit.Test
import org.junit.runner.RunWith

import org.junit.Assert.*
import java.util.concurrent.Executors

/**
 * Instrumented test, which will execute on an Android device.
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
@RunWith(AndroidJUnit4::class)
class ExampleInstrumentedTest {

    @Test
    fun useAppContext() {
        // Context of the app under test.
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        assertEquals("com.example.di", appContext.packageName)
        val controller = Controller()
        val fetcher = BusFetcher()
        fetcher.injectModule("")
        fetcher.injectReceiver(controller)
        Executors.newSingleThreadExecutor().submit {   fetcher.sendEvent(com.example.di.Test()) }.get()
    }
}
