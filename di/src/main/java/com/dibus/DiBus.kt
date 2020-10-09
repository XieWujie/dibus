package com.dibus

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import android.os.Handler
import android.os.Looper
import androidx.lifecycle.LifecycleOwner
import java.lang.RuntimeException
import kotlin.reflect.KClass

val diBus = DiBus()

class DiBus private constructor():Fetcher,EventDispatcher{


    val fetcher: Fetcher = DiBusFetcher()
    val eventDispatcher:EventDispatcher = EventHouse()



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
            return diBus.fetcher.fetch(name)
        }

        @JvmStatic
        fun registerScope(key:String, receiver: LifecycleOwner){
            return diBus.registerScope(key,receiver)
        }

        @JvmStatic
        fun register(key: String, register: DiFactory<*>){
            return diBus.fetcher.addFactory(key,register)
        }
        @JvmStatic
        fun register(key: String, receiver: Any){
            return diBus.addObjectWeak(key,receiver)
        }


        @JvmStatic
        fun postEvent(vararg args:Any){
            diBus.sendEvent(*args)
        }

        @JvmStatic
        fun register(receiver:Any):DiBus{
            val bus = getInstance()
            bus.addObjectWeak(receiver::class.java.canonicalName!!,receiver)
            return bus
        }


        @JvmStatic
        fun registerEvent(
            signature: String,
            eventExecutor: EventExecutor<Any>,
            receiver: Any
        ) {
            getInstance().eventDispatcher.observableEvent(signature, eventExecutor, receiver)
        }

        @JvmStatic
        fun findStickEvent(signature: String):Array<out Any>?{
            return getInstance().eventDispatcher.getStickEvent(signature)
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
       eventDispatcher.sendMessage(false,*args)
    }
    fun sendStickEvent(vararg args: Any){
        eventDispatcher.sendMessage(true,*args)
    }

    override fun addFactory(key: String, factory: DiFactory<*>) {
      fetcher.addFactory(key, factory)
    }

    override fun addObjectSingle(key: String, obj: Any) {
      fetcher.addObjectSingle(key, obj)
    }

    inline fun <reified T>scope(scope: String):T?{
        val name = T::class.java.canonicalName.toString()
        val scope = "$name&&$scope"
        return fetcher.fetch(scope) as T?
    }

    override fun addObjectWeak(key: String, obj: Any) {
        fetcher.addObjectSingle(key,obj)
    }


    override fun observableEvent(
        signature: String,
        eventExecutor: EventExecutor<Any>,
        receiver: Any
    ) {
        eventDispatcher.observableEvent(signature, eventExecutor, receiver)
    }

    override fun sendMessage(stick: Boolean, vararg args: Any) {
      eventDispatcher.sendMessage(false,args)
    }

    override fun getStickEvent(signature: String): Array<out Any>? {
     return  eventDispatcher.getStickEvent(signature)
    }

    fun injectApplication(context: Application){
        addObjectSingle(Context::class.java.canonicalName!!,context)
        addObjectSingle(Application::class.java.canonicalName!!,context)
        val sharedPreferences = context.getSharedPreferences(context.packageName,Context.MODE_PRIVATE)
        addObjectSingle(SharedPreferences::class.java.canonicalName!!,sharedPreferences)
    }

    inline fun<reified T:LifecycleOwner> scope(kClass: KClass<T>):T{
        val scope = T::class.java.simpleName.toString()
       return scope<T>(scope)?:throw RuntimeException("获取不到lifecycle owner")
    }


    fun registerScope(scope:String, receiver: Any){
        val name = receiver::class.java.canonicalName.toString()
        val s = Utils.buildScopeKey(name,scope)
        addObjectWeak(s,receiver)
    }
}