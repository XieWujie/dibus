package com.dibus

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import android.os.Handler
import android.os.Looper


class DiBus private constructor():Fetcher{


    private val fetcher: Fetcher = DiBusFetcher()
    private val eventDispatcher:EventDispatcher = EventHouse()



    companion object{
       @Volatile
       private var instance:DiBus? = null

        @JvmField
       internal val androidHandler = Handler(Looper.getMainLooper())


        @JvmStatic
        fun getInstance() =
            instance?: synchronized(DiBus::class.java){
                instance?:DiBus().also { this.instance = it }
            }

        @JvmStatic
        fun get(name:String):Any?{
            return getInstance().fetcher.fetch(name)
        }

        @JvmStatic
        fun registerScope(key:String, receiver: Any){
            return getInstance().fetcher.addObjectWeak(receiver::class.java.canonicalName!!+"&&$key",receiver)
        }

        @JvmStatic
        fun register(key: String, register: DiFactory<*>){
            return getInstance().fetcher.addFactory(key,register)
        }
        @JvmStatic
        fun register(key: String, receiver: Any){
            return getInstance().addObjectWeak(key,receiver)
        }


        @JvmStatic
        fun postEvent(vararg args:Any){
            getInstance().sendEvent(*args)
        }

        @JvmStatic
        fun register(receiver:Any):DiBus{
            val bus = getInstance()
            bus.addObjectWeak(receiver::class.java.canonicalName!!,receiver)
            return bus
        }

        @JvmStatic
        fun injectApplication(context: Application){
            val fetcher = getInstance()
            fetcher.registerSingleTon(Context::class.java.canonicalName!!,context)
            fetcher.registerSingleTon(Application::class.java.canonicalName!!,context)
            val sharedPreferences = context.getSharedPreferences(context.packageName,Context.MODE_PRIVATE)
            fetcher.registerSingleTon(SharedPreferences::class.java.canonicalName!!,sharedPreferences)
        }

        @JvmStatic
        fun registerEvent(
            signature: String,
            eventExecutor: EventExecutor<Any>,
            receiver: Any
        ) {
            getInstance().eventDispatcher.registerEvent(signature, eventExecutor, receiver)
        }


        inline fun <reified T> load():T{
           return getInstance().fetch(T::class.java.canonicalName!!) as T
        }

        inline fun <reified T> loadOfNull():T? =  getInstance().fetch(T::class.java.canonicalName!!) as T?

        operator fun invoke():DiBus{
            return getInstance()
        }

    }


    override fun fetch(key: String): Any? {
      return  fetcher.fetch(key)
    }


     fun sendEvent(vararg args:Any){
       eventDispatcher.sendEvent(*args)
    }

    override fun addFactory(key: String, factory: DiFactory<*>) {
      fetcher.addFactory(key, factory)
    }

    override fun addObjectSingle(key: String, obj: Any) {
      fetcher.addObjectSingle(key, obj)
    }

    override fun addObjectWeak(key: String, obj: Any) {
        fetcher.addObjectSingle(key,obj)
    }

    fun registerSingleTon(key: String,receiver: Any){
        fetcher.addObjectSingle(key,receiver)
    }
}