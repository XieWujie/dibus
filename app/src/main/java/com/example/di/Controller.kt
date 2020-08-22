package com.example.di

import com.xie.di.*

@Service(CREATE_PER)
class Controller {

    @AutoWire
    fun b(b: A){
        println("autoWire"+b)
    }

    @BusEvent(threadPolicy = THREAD_POLICY_MAIN)
    public fun event(test: Test){
        println("event:"+Thread.currentThread().toString())
    }
}

class Test()

class A @Service(CREATE_PER) constructor(b: B){

    init {
        println("init A")
    }
}

@Service(CREATE_PER)
class B{
    init {
        println("init B")
    }
}