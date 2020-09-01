package com.dibus

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.OnLifecycleEvent

interface LifeCycleProvide :LifecycleObserver{

    @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    fun onClear()

}

open class AndroidLifeCycleProvide(lifecycleOwner: LifecycleOwner){

    private val lifecycle = lifecycleOwner.lifecycle

    fun provide(dispose:()->Unit){

        val lifeCycleProvide = object :LifeCycleProvide{
            override fun onClear() {
                dispose.invoke()
            }
        }
        lifecycle.addObserver(lifeCycleProvide)
    }
}