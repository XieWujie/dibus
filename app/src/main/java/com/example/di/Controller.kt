package com.example.di

import com.xie.di.*
import java.io.Closeable
import java.io.Serializable


class Controller {

    @AutoWire
    fun u(closeable: Closeable){

    }

    @AutoWire
    fun c(c: C){

    }

}

open class Parent :Serializable,Cloneable

class Test()

class A @Service constructor(b: B):Parent(),Closeable{



    override fun close() {

    }
    @Provide
    public fun getC() :C{

        return C()
    }

}

@Service(CREATE_PER)
class B{

}
class C{

}