package com.xie.di.di

import com.xie.di.di_compiler.Fetcher

class Bus private constructor(){


    private val fetcher: Fetcher = BusFetcher()


    companion object{
       @Volatile
       private var instance:Bus? = null


        fun getInstance() =
            instance?: synchronized(Bus::class.java){
                instance?:Bus().also { this.instance = it }
            }

        fun register(receiver:Any):Bus{
            val bus = getInstance()
            bus.fetcher.injectReceiver(receiver)
            return bus
        }

        fun addModelInfo(vararg modelName:String){
            val fetcher = getInstance().fetcher
           for(model in modelName){
               fetcher.injectModule(model)
           }
        }

        operator fun invoke():Bus{
            return getInstance()
        }
    }

    fun sendEvent(vararg args:Any){
       fetcher.sendEvent(*args)
    }
}