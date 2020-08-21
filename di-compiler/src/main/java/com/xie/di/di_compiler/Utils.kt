package com.xie.di.di_compiler

import com.squareup.javapoet.ClassName
import java.lang.reflect.*
import java.lang.reflect.Array
import java.util.*
import javax.lang.model.element.Element
import javax.lang.model.element.ExecutableElement

object Utils {

    fun getClassNameFromPath(path:String):Pair<String,String>{
        val index = path.lastIndexOf(".")
        if(index == -1){
            throw IllegalArgumentException(path)
        }
        val pak = path.substring(0,index)
        val name = path.substring(index+1)
        return Pair(pak,name)
    }

    fun getParameterUpperBound(
        index: Int,
        type: ParameterizedType
    ): Type {
        val types = type.actualTypeArguments
        require(!(index < 0 || index >= types.size)) { "Index " + index + " not in range [0," + types.size + ") for " + type }
        val paramType = types[index]
        return if (paramType is WildcardType) {
            paramType.upperBounds[0]
        } else paramType
    }

    fun getSignatureFromArgs(args:kotlin.Array<out Any>):String{
        if(args.size == 1){
            return args[0]::class.java.canonicalName
        }
        val builder = StringBuilder()
        for(arg in args){
            builder.append(arg::class.java.canonicalName)
            builder.append(",")
        }
        if(args.isNotEmpty()){
            builder.deleteCharAt(builder.length-1)
        }
        return builder.toString()
    }




    fun getSignature(e:Element):String{
        if(e is ExecutableElement) {
            val signature = e.toString()
            return signature.substring(signature.indexOfFirst { '(' == it } + 1, signature.indexOfLast { ')' == it })
        }
        return ""
    }

    fun getFieldFromSignature(signature:String):List<String>{
        return signature.split(",")
    }

    fun getClassName(path:String):ClassName{
        val (pkg,name) = getClassNameFromPath(path)
        return ClassName.get(pkg,name)
    }
}