package com.example.di

import com.dibus.AutoWire
import com.dibus.Provide
import com.dibus.Service


@Service
class Test {

    @Provide()
    fun provide(name:String):String{
        return "xiewujie"
    }


}



class Need{

    @AutoWire()
    lateinit var userName: String
}
