package com.dibus

import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModel
import kotlin.reflect.KClass


fun LifecycleOwner.provide() = AndroidLifeCycleProvide(this)

fun LifecycleOwner.sendMessage(vararg args:Any){
    diBus.sendEvent(args)
}

fun ViewModel.sendMessage(vararg args: Any){
    diBus.sendEvent(args)
}

inline fun<reified T:LifecycleOwner> ViewModel.scope(kClass: KClass<T>):T{
   return diBus.scope<T>(kClass)
}

