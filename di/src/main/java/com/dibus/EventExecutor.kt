package com.dibus

interface EventExecutor<in T> {

    fun execute(receiver:T,vararg args:Any)

}

abstract class AndroidEventExecutor<T>(private val threadPolicy:Int):EventExecutor<T>{

    protected val handler = DiBus.androidHandler

    override fun execute(receiver:T,vararg obj:Any) {
        when(threadPolicy){
            THREAD_POLICY_MAIN->handler.post { realExecutor(receiver,*obj) }
            THREAD_POLICY_DEFAULT->realExecutor(receiver,*obj)
        }
    }

    abstract fun realExecutor(receiver: T,vararg args:Any)

}
