package com.example.di

import com.xie.di.BusEvent
import com.xie.di.BusFetcher
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.junit.Test

import org.junit.Assert.*

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class ExampleUnitTest {

    @Test
    fun main(){
        val l = System.currentTimeMillis()
        val n = Need()
        n.test()
        println(System.currentTimeMillis()-l)

        val fetcher = BusFetcher()
        fetcher.injectModule("")
        val u = System.currentTimeMillis()
        fetcher.injectReceiver(Controller())
        println(System.currentTimeMillis()-u)
        val c = C()
        var ll = System.currentTimeMillis()
        for(i in 0..10)
        EventBus.getDefault().post(Controller())
        println(System.currentTimeMillis()-ll)
        fetcher.injectReceiver(c)
        ll = System.currentTimeMillis()
        for(i in 0..10)
        fetcher.sendEvent(Controller())
        println(System.currentTimeMillis()-ll)
    }



    class C(){
        init {
            EventBus.getDefault().register(this)
        }
        @Subscribe
        @BusEvent
        fun s(c:Controller){

        }
    }
}
