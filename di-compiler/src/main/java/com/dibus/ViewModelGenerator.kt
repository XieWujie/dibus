package com.dibus

import com.squareup.javapoet.*
import java.io.IOException
import javax.annotation.processing.Filer
import javax.lang.model.element.Modifier

internal class ViewModelGenerator (private val filer: Filer, private val info: BusAwareInfo, private val createMethod: MethodSpec,private val moduleName:String){

    val name:String ="${Utils.getClassNameFromPath(info.receiverClass).second}Factory"

    private fun generateCreate():MethodSpec{
        val viewModelType = ClassName.get("androidx.lifecycle","ViewModel")
        val returnType = TypeVariableName.get("T",viewModelType)
        val pType = ParameterizedTypeName.get(ClassName.get(Class::class.java),returnType)
        return MethodSpec.methodBuilder("create")
            .addAnnotation(Override::class.java)
            .addTypeVariable(returnType)
            .addModifiers(Modifier.PUBLIC)
            .addParameter(pType,"modelType")
            .returns(returnType)
            .addStatement("return (\$T)create()",returnType)
            .build()
    }



    fun generate(){
        val instanceType =Utils.getClassName(info.receiverClass)

        val instance = FieldSpec.builder(instanceType,"instance").build()
        val superClass = ClassName.get("androidx.lifecycle.ViewModelProvider","NewInstanceFactory")

        val type = TypeSpec.classBuilder(name)
            .addField(instance)
            .addMethod(createMethod)
            .addMethod(generateCreate())
            .superclass(superClass)
            .build()
        try {
            JavaFile.builder("$BASE_PACKAGE.$moduleName", type).build()
                .writeTo(filer)
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

}