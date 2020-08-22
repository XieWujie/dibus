package com.example.di

import com.xie.di.*

@Service(CREATE_SCOPE)
class Controller {

    @AutoWire
     fun a(a: A,b: B,c: C){
        println("autoWire abc"+a+b+c)
    }

    @AutoWire
    fun c(c: C){
        println("autoWire c:"+c)
    }

    @BusEvent(threadPolicy = THREAD_POLICY_MAIN)
    public fun event(test: Test){
        println("event:"+Thread.currentThread().toString())
    }
}

class Test()

class A @Service constructor(b: B){

    init {
        println("init A")
    }

    @Provide
    public fun getC() :C{
        println("getC")
        return C()
    }

}

@Service(CREATE_PER)
class B{
    init {
        println("init B")
    }
}
class C{
    init {
        println("init C")
    }
}