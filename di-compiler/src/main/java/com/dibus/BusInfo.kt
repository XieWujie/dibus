package com.dibus

data class BusEventInfo(val functionName:String, val argsSignature:String, val thread:Int,val stick:Boolean = false)

internal data class ServiceInfo(val argsSignature: String,val viewOwnerFrom: String? = null)

internal data class EventMete(val receiver:String,val thread:Int,val isStick:Boolean = false)

internal data class AutoWireInfo(val name: String,val argsSignature: String,val wireType:Int,val scope:String? = null)

internal data class StaticInfo(val ClazzName:String,val methodName:String)

internal data class RegisterInfo(val receiver: String,val isType:Boolean,val functionName: String? = null,val createStrategy: Int)

internal data class ProvideInfo(val returnType:String,val functionName: String,val scope: String? = null,val createStrategy: Int,val module:String = "")

internal const val FUNCTION = 1
internal const val FIELD = 0



internal data class BusAwareInfo(val receiverClass: String,
                        val autoWire: ArrayList<AutoWireInfo>,var service:ServiceInfo?,
                        val busEvent:ArrayList<BusEventInfo>,
                        val registerInfo: ArrayList<RegisterInfo>,
                         val provideInfo:ArrayList<ProvideInfo>,var createStrategy: Int = CREATE_SCOPE)

