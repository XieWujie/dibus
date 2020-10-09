package com.dibus


interface EventDispatcher{

    fun observableEvent(signature:String, eventExecutor: EventExecutor<Any>, receiver:Any)

    fun sendMessage(stick:Boolean = false, vararg args:Any)

    fun getStickEvent(signature: String):Array<out Any>?
}
 class EventHouse:EventDispatcher {

    private val events = HashMap<String,WeakEventQueue<Any>>()
    private val stickEvents = HashMap<String,Array<out Any>>()


    override fun  observableEvent(
        signature: String,
        eventExecutor: EventExecutor<Any>,
        receiver: Any
    ) {
        val q = getQueue(signature)
        q.add(receiver,eventExecutor)
    }


     override fun getStickEvent(signature: String): Array<out Any>? {
         return stickEvents[signature]
     }
    private fun getQueue(key:String) = events[key]?:WeakEventQueue<Any>().also { events[key] = it }

    override fun sendMessage(stick:Boolean, vararg args:Any) {
        val signature = Utils.getSignatureFromArgs(args)
        if(stick) stickEvents[signature] = args
        val q = events[signature]?:return
        if(q.size == 0)events.remove(signature)
        for(e in q.iterator()){
            e.execute(*args)
        }
    }

}