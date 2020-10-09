package com.dibus

interface EventExecutor<in T> {

    fun execute(receiver:T,vararg args:Any)

}

abstract class AndroidEventExecutor<T>(private val threadPolicy:Int):EventExecutor<T>{

    private val handler = DiBus.androidHandler


    override fun execute(receiver:T,vararg args:Any) {
        when(threadPolicy){
            THREAD_POLICY_MAIN->handler.post { realExecutor(receiver,*args) }
            THREAD_POLICY_DEFAULT->realExecutor(receiver,*args)
        }
    }

    abstract fun realExecutor(receiver: T,vararg args:Any)

}
