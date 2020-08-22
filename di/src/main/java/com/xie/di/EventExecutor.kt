package com.xie.di

import android.os.Handler
import android.os.Looper

interface EventExecutor {

    fun execute(creator: BusCreator<*>,args: List<Any>,mete: EventMete)

}

class AndroidEventExecutor:EventExecutor{

    private val handler = Handler(Looper.getMainLooper())

    override fun execute(creator: BusCreator<*>, args: List<Any>, mete: EventMete) {
        when(mete.thread){
            THREAD_POLICY_MAIN->{
                handler.post{
                    creator.eventAware(args)
                }
            }
            else-> creator.eventAware(args)
        }

    }

}

class DefaultExecutor:EventExecutor{

    override fun execute(creator: BusCreator<*>, args: List<Any>, mete: EventMete) {
         creator.eventAware(args)
    }
}