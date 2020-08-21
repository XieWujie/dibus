package com.xie.di.di


import com.xie.di.di_compiler.*
import java.lang.ref.Reference
import java.lang.ref.ReferenceQueue
import java.lang.ref.WeakReference
import java.util.*
import kotlin.collections.HashMap

 class BusFetcher : Fetcher {

    /**
     * 存放CREATE_PER用户注册的接收者对象
     */
    private val referenceQueues = HashMap<String, ReferenceQueue<Any>>()

    /**
     * 存放各种接收者类对应的对象
     */
    private val creators = WeakHashMap<String, BusCreator<*>>()

    /**
     * 每个模块都有一个BusCreatorFetcher,用于传教creator
     */
    private val busCreatorFetchers = HashMap<String, BusCreatorFetcher>()

    /**
     * 存放被@service和能被@provide传教的类的信息，@service会覆盖@provide
     */
    private val typeMetes = HashMap<String, TypeMete>()

    private val creatorsSingleTon = HashMap<String, BusCreator<*>>()
    private val provideSingleTon = HashMap<String, Any>()

    /**
     * 所有事件的容器，一个事件可以对应多个接受者的类
     */
    private val events = HashMap<String, ArrayList<String>>()

    override fun fetch(key: String): Any? {
        val createStrategy = if (typeMetes.containsKey(key)) {
            typeMetes[key]!!.createStrategy
        } else {
            CREATE_SCOPE
        }
        return when (createStrategy) {
            CREATE_PER -> getNewObject(key)
            CREATE_SCOPE -> getScope(key)
            CREATE_SINGLETON -> getSingleTon(key)
            else -> throw IllegalArgumentException("传入的createStrategy不能处理")
        }
    }

    /**
     * 添加对象进入事件处理队列
     */
    private fun enqueue(key: String, receiver: Any) {
        val qu = if (referenceQueues.containsKey(key)) {
            referenceQueues[key]!!
        } else {
            ReferenceQueue<Any>().apply { referenceQueues[key] = this }
        }
        WeakReference<Any>(receiver, qu).enqueue()
    }

    override fun injectModule(name: String) {
        try {
            val m = Class.forName("$BASE_PACKAGE.$BUS_PREFIX$name").newInstance() as BusCreatorFetcher
            busCreatorFetchers[name] = m
            m.loadTypeMete(typeMetes)
        } catch (e: ClassNotFoundException) {
            e.printStackTrace()
        }
    }

    override fun sendEvent(vararg args: Any) {
        val s = Utils.getSignatureFromArgs(args)
        if (!events.containsKey(s)) {
            return
        }
        val a = events[s]!!
        for (w in a) {
            val creator = when {
                creators.containsKey(w) -> creators[w]
                creatorsSingleTon.containsKey(w)->creatorsSingleTon[w]
                else->null
            } ?: continue
            executeEvent(creator,args.toList(),w)
        }
    }

    private fun executeEvent(creator: BusCreator<*>,args: List<out Any>,key: String){
        if(referenceQueues.containsKey(key)){
           val queue = referenceQueues[key]!!
            val newQueue = ReferenceQueue<Any>()
            var r:Reference<*>? = queue.poll()
            while (r != null){
                val receiver = r.get()?:continue
                creator.setReceiver(receiver)
                creator.eventAware(args)
                WeakReference(receiver,newQueue).enqueue()
                r= queue.poll()
            }
            referenceQueues[key] = newQueue
        }else{
            creator.eventAware(args)
        }
    }

    private fun getNewObject(key: String): Any? {

        return if (creators.containsKey(key)) {
            creators[key]!!.create()?.apply {
                enqueue(key, this)
            }
        } else {
            findNew(key)
        }
    }

    private fun getSingleTon(key: String): Any? {
        if (provideSingleTon.containsKey(key)) {
            return provideSingleTon[key]
        }
        if (creatorsSingleTon.containsKey(key)) {
            return creatorsSingleTon[key]
        }
        return findNew(key)
    }

    private fun getScope(key: String): Any? {
        if (creators.containsKey(key)) {
            return creators[key]?.getReceiver()
        }
        if (referenceQueues.contains(key)) {
            val que = referenceQueues[key]!!
            var target  = que.poll()
            while (target != null){
                val t = target?.get()
                if(t != null){
                    WeakReference(t,que).enqueue()
                    return t
                }
                target = que.poll()
            }
            return findNew(key)
        }
        return findNew(key)
    }

    private fun findNew(key: String, receiver: Any? = null): Any? {
        val creator = getFromFetcher(key, receiver)
        val mete = typeMetes[key]
        val strategy = mete?.createStrategy?: CREATE_SCOPE
        if (creator != null) {
            when (strategy) {
                CREATE_SINGLETON -> creatorsSingleTon[key] = creator
                CREATE_SCOPE -> creators[key] = creator
                CREATE_PER->creators[key] = creator.apply { enqueue(key,this) }
            }
            return creator.getReceiver()
        } else {
            val otherKey = mete?.canProvideFrom?:return null
            val provideCreator = creators[otherKey]?: getFromFetcher(otherKey, receiver)
                    ?: return null
            val real = receiver ?: provideCreator.provide(key) ?: return null
            when (strategy) {
                CREATE_SINGLETON -> provideSingleTon[key] = real
                CREATE_SCOPE , CREATE_PER-> enqueue(key, real)
            }
            return real
        }
    }

    private fun getFromFetcher(key: String, receiver: Any? = null): BusCreator<*>? {
        if ((typeMetes.containsKey(key) && typeMetes[key]!!.isService) || receiver != null)
            for ((_, value) in busCreatorFetchers) {
                val v = value.fetch(key, this, receiver)
                if (v != null) {
                    v.supportEventType(events)
                    return v
                }
            }
        return null
    }


    override fun injectReceiver(any: Any) {
        val key = any::class.java.canonicalName!!
        enqueue(key,any)
        when {
            creators.containsKey(key) -> {
                creators[key]?.apply {
                    setReceiver(any)
                }
            }
            creatorsSingleTon.containsKey(key) -> {
                creatorsSingleTon[key]?.apply { setReceiver(any) }
            }
            else -> {
                findNew(key, any)
            }
        }
    }

}