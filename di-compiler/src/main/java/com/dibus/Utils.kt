package com.dibus

import com.squareup.javapoet.ClassName
import java.util.*
import javax.lang.model.element.Element
import javax.lang.model.element.ExecutableElement
import kotlin.reflect.KClass

internal object Utils {

    fun getClassNameFromPath(path:String):Pair<String,String>{
        val index = path.lastIndexOf(".")
        if(index == -1){
            throw IllegalArgumentException(path)
        }
        val pak = path.substring(0,index)
        val name = path.substring(index+1)
        return Pair(pak,name)
    }

    fun getSignatureFromArgs(args: Array<out Any>):String{
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

    fun getClassFromAnnotation(from:KClass<*>,element: Element,key:String): String? {
        val annotationMirrors = element.annotationMirrors
        for (annotationMirror in annotationMirrors) {
            if (from.java.name == annotationMirror.annotationType.toString()) {
                val keySet = annotationMirror.elementValues.keys
                for (executableElement in keySet) {
                    if (Objects.equals(executableElement.simpleName.toString(), key)) {
                        return annotationMirror.elementValues[executableElement]!!.value.toString()
                    }
                }
            }
        }
        return null
    }

    fun getClassName(path:String):ClassName{
        val (pkg,name) = getClassNameFromPath(path)
        return ClassName.get(pkg,name)
    }

    fun buildScopeKey(typeName:String,scope:String?):String{
        return if(scope.isNullOrEmpty()) typeName
        else  "$typeName&&$scope"
    }

}