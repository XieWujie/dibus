package com.dibus


interface EventDispatcher{

    fun registerEvent(signature:String,eventExecutor: EventExecutor<Any>,receiver:Any)

    fun sendEvent(vararg args:Any)
}
internal class EventHouse:EventDispatcher {

    private val events = HashMap<String,WeakEventQueue<Any>>()


    override fun  registerEvent(
        signature: String,
        eventExecutor: EventExecutor<Any>,
        receiver: Any
    ) {
        val q = getQueue(signature)
        q.add(receiver,eventExecutor)
    }

    private fun getQueue(key:String) = events[key]?:WeakEventQueue<Any>().also { events[key] = it }

    override fun sendEvent(vararg args: Any) {
        val signature = Utils.getSignatureFromArgs(args)
        val q = events[signature]?:return
        if(q.size == 0)events.remove(signature)
        for(e in q.iterator()){
            e.execute(*args)
        }
    }

}