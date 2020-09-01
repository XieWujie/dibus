package com.example.di

import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.runner.AndroidJUnit4
import com.xie.di.BUS_PREFIX
import com.xie.di.Bus
import com.xie.di.BusFetcher

import org.junit.Test
import org.junit.runner.RunWith
import com.xie.di.AutoWire
import com.xie.di.Provide
import com.xie.di.Service

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
    fun main(){
        val fetcher = BusFetcher()
        fetcher.injectModule("")
        val need = Need()
        fetcher.injectReceiver(need)
        println(need.userName)

    }

}

