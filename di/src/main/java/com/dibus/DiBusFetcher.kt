package com.dibus

import java.util.*
import kotlin.collections.HashMap

interface Fetcher{

    /**
     * 从容器中获取实例，如果是@service标识的类可以自动创建
     */
    fun fetch(key:String):Any?

    fun addFactory(key: String,factory: DiFactory<*>)

    fun addObjectSingle(key: String,obj:Any)

    fun addObjectWeak(key: String,obj: Any)

}
class DiBusFetcher internal constructor() :Fetcher{

    private val factories = HashMap<String,DiFactory<*>>()

    private val runWeakObject = WeakHashMap<String,Any>()

    private val runObject = HashMap<String,Any>()


    override fun fetch(key: String): Any? {
        return runObject[key]?:
                runWeakObject[key]?:
                factories[key]?.get()
    }

    override fun addFactory(key: String, factory: DiFactory<*>) {
        factories[key] = factory
    }

    override fun addObjectSingle(key: String, obj: Any) {
        runObject[key] = obj
    }

    override fun addObjectWeak(key: String, obj: Any) {
       runWeakObject[key] = obj
    }
}