package com.xie.di

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
     var executor:EventExecutor = AndroidEventExecutor()

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
    private val events = HashMap<String, ArrayList<EventMete>>()

    override fun fetch(key: String): Any? {
        var k = key
        val createStrategy = if (typeMetes.containsKey(key)) {
            typeMetes[key]!!.run {
                if(isService)k = this.canProvideFrom
                createStrategy
            }
        } else {
            CREATE_SCOPE
        }
        return when (createStrategy) {
            CREATE_PER -> getNewObject(k)
            CREATE_SCOPE -> getScope(k)
            CREATE_SINGLETON -> getSingleTon(k)
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

    override fun injectModule(name: String ) {
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
                creators.containsKey(w.receiver) -> creators[w.receiver]
                creatorsSingleTon.containsKey(w.receiver)->creatorsSingleTon[w.receiver]
                else->null
            } ?: continue
            executeEvent(creator,args.toList(),w)
        }
    }

    private fun executeEvent(creator: BusCreator<*>,args: List<Any>,mete: EventMete){
        val key = mete.receiver
        if(referenceQueues.containsKey(key)){
           val queue = referenceQueues[key]!!
            //换个队列存储接收者若采用原来的队列会造成死循环
            val newQueue = ReferenceQueue<Any>()
            var r:Reference<*>? = queue.poll()
            while (r != null){
                val receiver = r.get()?:continue
                creator.setReceiver(receiver)
                //执行线程调度
                executor.execute(creator,args,mete)
                //进入新队列
                WeakReference(receiver,newQueue).enqueue()
                r= queue.poll()
            }
            //更换队列，原来的队列已空
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
     override fun injectModule(moduleName:String, busCreatorFetcher: BusCreatorFetcher) {
         busCreatorFetchers[moduleName] = busCreatorFetcher
     }

     /**
      * 从两个强引用队列拿取
      * 没有就创建新的
      */
    private fun getSingleTon(key: String): Any? {
        if (provideSingleTon.containsKey(key)) {
            return provideSingleTon[key]
        }
        if (creatorsSingleTon.containsKey(key)) {
            return creatorsSingleTon[key]
        }
        return findNew(key)
    }

     /**
      * 从弱引用队列获取哦，没有就获取新的
      */
    private fun getScope(key: String): Any? {
        if (creators.containsKey(key)) {
            return creators[key]?.getReceiver()
        }
        if (referenceQueues.contains(key)) {
            val que = referenceQueues[key]!!
            var target  = que.poll()
            while (target != null){
                val t = target.get()
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

     /**
      *
      * @param receiver 若为空则表示要获取的真实目标是接收者，不为空则是
      * @see BusCreator
      */
    private fun findNew(key: String, receiver: Any? = null): Any? {
        val creator = getFromFetcher(key, receiver)
        val mete = typeMetes[key]
        val strategy = mete?.createStrategy?: CREATE_SCOPE
        if (creator != null) {
            when (strategy) {
                CREATE_SINGLETON -> creatorsSingleTon[key] = creator
                CREATE_SCOPE -> creators[key] = creator
                CREATE_PER->creators[key] = creator.apply {  receiver?:enqueue(key,this.getReceiver()!!) }
            }
            return creator.getReceiver()
        } else {
            receiver?.apply { return this }
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

     /**
      * 获取
      * @see BusCreator
      */
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
                    autoWire()
                }
            }
            creatorsSingleTon.containsKey(key) -> {
                creatorsSingleTon[key]?.apply {
                    setReceiver(any)
                    autoWire()
                }
            }
            else -> {
                findNew(key, any)
            }
        }
    }

}