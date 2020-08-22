package com.example.di

import com.xie.di.BusFetcher
import org.junit.Test

import org.junit.Assert.*

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class ExampleUnitTest {
    @Test
    fun addition_isCorrect() {
        val controller = Controller()
        val fetcher = BusFetcher()
        fetcher.injectModule("")
        fetcher.injectReceiver(controller)
      //  fetcher.injectReceiver(Controller())
        fetcher.sendEvent(com.example.di.Test())
        fetcher.sendEvent(com.example.di.Test())
    }
}
