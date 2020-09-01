package com.dibus

import com.google.auto.service.AutoService
import com.squareup.javapoet.*
import java.io.IOException
import java.lang.ref.WeakReference
import javax.annotation.processing.*
import javax.lang.model.element.*

@Suppress("MISSING_DEPENDENCY_CLASS")
@AutoService(Processor::class)
class DiBusProcessor : AbstractProcessor() {

    private lateinit var filer: Filer
    private lateinit var moduleName: String
    private val staticInfo = HashMap<String,StaticInfo>()
    private val busInfos = HashMap<String, BusAwareInfo>()
    private val dibusType = ClassName.get(DIBUS_PAC,"DiBus")
    override fun getSupportedAnnotationTypes() = setOf(
        BusEvent::class.java.canonicalName,
        AutoWire::class.java.canonicalName,
        Service::class.java.canonicalName,
        Scope::class.java.canonicalName,
        Provide::class.java.canonicalName,
        ViewModelService::class.java.canonicalName,
        Register::class.java.canonicalName
    )

    override fun init(processing: ProcessingEnvironment) {
        filer = processing.filer
        moduleName = processing.options["moduleName"] ?: "default"
    }

    override fun process(p0: MutableSet<out TypeElement>?, p1: RoundEnvironment): Boolean {
        val funcElements = p1.getElementsAnnotatedWith(BusEvent::class.java)
        for (e in funcElements) {
            if (e is ExecutableElement) {
                fetchFunctionInfo(e)
            }
        }
        val serviceElements = p1.getElementsAnnotatedWith(Service::class.java)
        for (e in serviceElements) {
            fetchServiceInfo(e)
        }
        val viewModel = p1.getElementsAnnotatedWith(ViewModelService::class.java)
        for( e in viewModel){
            fetchViewModelInfo(e)
        }
        val autoWireElements = p1.getElementsAnnotatedWith(AutoWire::class.java)
        for (e in autoWireElements) {
            fetchAutoWireInfo(e)
        }
        val provideElements = p1.getElementsAnnotatedWith(Provide::class.java)
        for (e in provideElements) {
            if (e is ExecutableElement) {
                fetchProvideInfo(e)
            }
        }
        val scopeElement = p1.getElementsAnnotatedWith(Scope::class.java)
        for(e in scopeElement){
            fetchScopeInfo(e)
        }
        val registerElement = p1.getElementsAnnotatedWith(Register::class.java)
        for(e in registerElement){
            getRegisterInfo(e)
        }
        getStaticAutoInfo()
        for (info in busInfos) {
            creatorHandler(info.value)
        }
        busInfos.clear()
        staticInfo.clear()
        return true
    }

    private fun getRegisterInfo(e: Element){
        val createStrategy = e.getAnnotation(Register::class.java).createStrategy
        val canprovide:String
        val t = if(e is TypeElement){
            canprovide = e.qualifiedName.toString()
            RegisterInfo(e.qualifiedName.toString(),true,null,createStrategy)
        }else if(e is ExecutableElement){
            canprovide = e.enclosingElement.toString()
            RegisterInfo(e.returnType.toString(),false,e.simpleName.toString(),createStrategy)
        }else{
            return
        }
        val info = busAwareInfo(canprovide)
        info.registerInfo.add(t)
    }

    private fun getStaticAutoInfo(){
      for((_,value) in busInfos){
          staticInfo[value.receiverClass] = StaticInfo(value.receiverClass,"get")
          for(i in value.provideInfo){
              val key = Utils.buildScopeKey(i.returnType,i.scope)
              staticInfo[key] = StaticInfo(value.receiverClass,i.functionName)
          }
      }
    }


    private fun fetchScopeInfo(e: Element){
        val scope = e.getAnnotation(Scope::class.java).value
        if(e is VariableElement){
            fetchAutoWireInfo(e,scope)
        }else if (e is ExecutableElement){
            if(e.typeParameters.size>0){
                fetchAutoWireInfo(e,scope)
                return
            }
            if(e.returnType.kind.toString() !="VOID"){
                fetchProvideInfo(e,scope)
            }
        }
    }

    private fun fetchViewModelInfo(e: Element){
        val viewOwner = Utils.getClassFromAnnotation(ViewModelService::class,e,"viewOwner")
        fetchServiceInfo(e,viewOwner)
    }



    private fun fetchProvideInfo(e: ExecutableElement,scope: String? = null) {
        val key = e.enclosingElement.toString()
        val info = busAwareInfo(key)
        val createStrategy = e.getAnnotation(Provide::class.java)?.createStrategy
            ?:e.getAnnotation(Scope::class.java).createStrategy
        val returnType =  e.returnType.toString()
        val provide = ProvideInfo(returnType, e.simpleName.toString(),scope,createStrategy)
        info.provideInfo.add(provide)
    }

    private fun fetchAutoWireInfo(e: Element,scope: String? = null) {
        val info = busAwareInfo(e.enclosingElement.toString())
        if (e is VariableElement) {
            info.autoWire.add(AutoWireInfo(e.toString(), e.asType().toString(), FIELD,scope))
        } else if (e is ExecutableElement) {
            val arg = Utils.getSignature(e)
            info.autoWire.add(
                AutoWireInfo(
                    e.simpleName.toString(),
                    arg,
                    FUNCTION,
                    scope
                )
            )
        }

    }



    private fun registerEventMethod(info: BusAwareInfo,instanceType: ClassName): MethodSpec {

        val method = MethodSpec.methodBuilder("registerEvents")
            .addModifiers(Modifier.STATIC)
            .addParameter(instanceType,"receiver")
        val executorType = ClassName.get(DIBUS_PAC,"AndroidEventExecutor")
        for(event in info.busEvent){
            val argsType = Utils.getFieldFromSignature(event.argsSignature).map { Utils.getClassName(it) }.toTypedArray()
            val builder = StringBuilder()
            for(t in argsType.indices){
                builder.append("(\$T)args[$t],")
            }
            builder.deleteCharAt(builder.length-1)
          val c = "\$T ${event.functionName} =  new \$T<\$T>(${event.thread}){\n" +
                  "      @Override\n" +
                  "      public void realExecutor(\$T receiver,Object ...args) {\n" +
                  "        receiver.${event.functionName}(${builder.toString()});\n" +
                  "      }\n" +
                  "    }\n"
            method.addStatement(c,executorType,executorType,instanceType,instanceType,*argsType)
                .addStatement("\$T.registerEvent(\$S,${event.functionName},receiver)",dibusType,event.argsSignature)
        }
        return method.build()
    }

    private fun registerMethod(info: BusAwareInfo):CodeBlock{
        val code = CodeBlock.builder()
        val canProvideType = Utils.getClassNameFromPath(info.receiverClass).second+ CREATOR
        val diFactoryType = ClassName.get(DiFactory::class.java)
        for(i in info.registerInfo){
           val func =  if(i.isType) "" else ".${i.functionName}()"
            val name = i.functionName?:"creator"
            when(i.createStrategy){
                CREATE_PER->{
                    val c =  "\$T $name = new \$T(){\n" +
                            "\n" +
                            "    @Override\n" +
                            "    public Object provide() {\n" +
                            "      return $canProvideType.get()$func;\n" +
                            "    }\n" +
                            "  }"
                    code.addStatement(c,diFactoryType,diFactoryType)
                        .addStatement("\$T.register(\$S,$name)",dibusType,i.receiver)
                }
                CREATE_SCOPE->{
                    val referenceType = ClassName.get(WeakReference::class.java)
                    val c =  "\$T $name = new \$T(){\n" +
                            " \$T reference;\n"+
                            "\n" +
                            "  @Override\n" +
                            "  public Object get() {\n" +
                            "   if(reference == null || reference.get()==null){\n"+
                            "    reference = new WeakReference($canProvideType.get()$func);\n}" +
                            "\n"+
                            "     return reference.get();\n" +
                            "   }\n" +
                            " }"
                    code.addStatement(c,diFactoryType,diFactoryType,referenceType)
                        .addStatement("\$T.register(\$S,$name)",dibusType,i.receiver)
                }
                CREATE_SINGLETON->{
                    val referenceType = Utils.getClassName(i.receiver)
                    val c =  "\$T $name = new \$T(){\n" +
                            " \$T reference;\n"+
                            "\n" +
                            "  @Override\n" +
                            "  public Object get() {\n" +
                            "   if(reference == null){\n"+
                            "    reference = $canProvideType.get()$func;\n}" +
                            "\n"+
                            "     return reference;\n" +
                            "   }\n" +
                            " }"
                    code.addStatement(c,diFactoryType,diFactoryType,referenceType)
                        .addStatement("\$T.register(\$S,$name)",dibusType,i.receiver)
                }
            }
        }
        return code.build()
    }


    private fun createIsViewModelMethod(info: BusAwareInfo):MethodSpec{
        val service = info.service
        if(service?.viewOwnerFrom.isNullOrBlank()){
            return createMethod(info)
        }
        val viewModelGenerator = ViewModelGenerator(filer,info,createMethod(info),moduleName)
        viewModelGenerator.generate()
        val factoryName = viewModelGenerator.name
        val factoryType = ClassName.get("$BASE_PACKAGE.$moduleName",factoryName)
        val instanceType = Utils.getClassName(info.receiverClass)
        val viewOwnerType = Utils.getClassName(service!!.viewOwnerFrom!!)
        val runtimeExceptionType = ClassName.get(RuntimeException::class.java)
        val viewModelProvideType = ClassName.get("androidx.lifecycle","ViewModelProvider")
        val creatorMethod = MethodSpec.methodBuilder("create")
            .addModifiers(Modifier.PUBLIC,Modifier.STATIC)
            .returns(instanceType)
            .addStatement("\$T factory = new \$T()",factoryType,factoryType)
            .addCode(fetchStrategy(service.viewOwnerFrom!!,"Object viewOwner"))
            .beginControlFlow("if(viewOwner == null)")
            .addStatement("throw new \$T(\$S)",runtimeExceptionType,"无法获取view owner:${service.viewOwnerFrom}")
            .endControlFlow()
            .addStatement("\$T receiver = new \$T((\$T)viewOwner,factory).get(\$T.class)",instanceType,viewModelProvideType,viewOwnerType,instanceType)
            .addStatement("return receiver")
        return creatorMethod.build()
    }

    private fun createMethod(info: BusAwareInfo): MethodSpec {
        //创建接收者的方法 create
        val instanceType = Utils.getClassName(info.receiverClass)

        val creatorMethod = MethodSpec.methodBuilder("create")
            .addModifiers(Modifier.PUBLIC,Modifier.STATIC)
            .returns(instanceType)
        val signature = info.service?.argsSignature
        creatorMethod
            .addStatement("\$T receiver",instanceType)
        if (signature.isNullOrEmpty()) {
            creatorMethod
                .addStatement("receiver = new \$T()",instanceType)
        } else {
            val types =
                Utils.getFieldFromSignature(signature).map { Utils.getClassName(it) }.toTypedArray()
            if (types.size == 1) {
                creatorMethod
                    .addCode(fetchStrategy(types[0].canonicalName(),"Object arg"))
                    .beginControlFlow("if(arg != null)")
                    .addStatement("receiver = new \$T((\$T)arg)",instanceType, types[0])
                    .endControlFlow()
            } else{
                multiArgsGenerate(
                    creatorMethod,
                    types,
                    "args",
                    "receiver = new ${instanceType.simpleName()}"
                )
            }
            val e = ClassName.get(RuntimeException::class.java)
            creatorMethod.beginControlFlow("else")
                .addStatement(
                    "throw new \$T(\$S)",
                    e,
                    "无法创建此类${instanceType.canonicalName()}，非空参数不足"
                )
                .endControlFlow()
        }

        if(info.service?.viewOwnerFrom.isNullOrEmpty()){
            creatorMethod.addStatement("inject(receiver)")
        }else{
            val type = ClassName.get("$BASE_PACKAGE.$moduleName","${instanceType.simpleName()}$CREATOR")
            creatorMethod.addStatement("\$T.inject(receiver)",type)
        }
            creatorMethod.addStatement("return receiver")
        return creatorMethod.build()
    }



    private fun multiArgsGenerate(
        methodSpec: MethodSpec.Builder,
        typeNames: Array<ClassName>,
        argsName: String,
        realTry: String
    ) {
        methodSpec.addStatement("Object $argsName[] = new Object[${typeNames.size}]")
        for (index in typeNames.indices) {
            methodSpec.addCode(fetchStrategy( typeNames[index].canonicalName(), "$argsName[$index]"))
        }
        val builder = StringBuilder()
        builder.append("if(")
        for (index in typeNames.indices) {
            builder.append("$argsName[$index] instanceof \$T &&")
        }
        builder.deleteCharAt(builder.length - 1)
        builder.deleteCharAt(builder.length - 1)
        builder.append(")")
        methodSpec.beginControlFlow(builder.toString(), *typeNames)
        builder.delete(0, builder.length)
        builder.append(realTry)
        builder.append("(")
        for (index in typeNames.indices) {
            builder.append("(\$T)$argsName[$index],")
        }
        builder.deleteCharAt(builder.length - 1)
        builder.append(")")
        methodSpec.addStatement(builder.toString(), *typeNames)
            .endControlFlow()
    }

    private fun fetchStrategy(typeKey:String, leftCode:String):CodeBlock{
       return if(staticInfo.containsKey(typeKey)){
            val value = staticInfo[typeKey]!!
           val name = Utils.getClassNameFromPath(value.ClazzName).second
            val type = ClassName.get("$BASE_PACKAGE.$moduleName","${name}$CREATOR")
           CodeBlock.builder().addStatement("$leftCode=\$T.${value.methodName}()",type).build()
        }else{
           CodeBlock.builder().addStatement("$leftCode=\$T.get(\$S)",dibusType,typeKey).build()
        }
    }

    private fun autoWireMethod(info: BusAwareInfo,receiver:ClassName): MethodSpec {

        //autoWire
        val autoWireMethod = MethodSpec.methodBuilder("autoWire")
          //  .addAnnotation(Override::class.java)
            .addModifiers(Modifier.PUBLIC,Modifier.STATIC)
            .addParameter(receiver,"instance")

        for (autoWire in info.autoWire) {
            val instanceType = Utils.getFieldFromSignature(autoWire.argsSignature)
            val sArgj = if(autoWire.scope == null) "null" else "\"${autoWire.scope}\""
            if (instanceType.size == 1) {
                val typeName = Utils.getClassName(instanceType[0])
                val returnName = "${autoWire.name}_fun"
                autoWireMethod.addCode(fetchStrategy(Utils.buildScopeKey(autoWire.argsSignature,autoWire.scope),"Object $returnName"))
                if (autoWire.wireType == FIELD) {
                    autoWireMethod.beginControlFlow("if($returnName != null)")
                        .addStatement("instance.${autoWire.name} = (\$T)$returnName", typeName)
                        .endControlFlow()
                } else if (autoWire.wireType == FUNCTION) {
                    autoWireMethod.beginControlFlow("if($returnName != null)")
                        .addStatement("instance.${autoWire.name}((\$T)$returnName)", typeName)
                        .endControlFlow()
                }
            } else {
                val typeNames = instanceType.map { Utils.getClassName(it) }.toTypedArray()
                val argsName = "${autoWire.name}_fun"
                multiArgsGenerate(autoWireMethod, typeNames, argsName, "instance.${autoWire.name}")
            }
        }
        return autoWireMethod.build()
    }

    private fun getReceiverField(busAwareInfo: BusAwareInfo,receiverType:ClassName):FieldSpec{

       val fieldType =  when(busAwareInfo.createStrategy){
            CREATE_PER, CREATE_SCOPE->ParameterizedTypeName.get(ClassName.get(WeakReference::class.java),receiverType)
           CREATE_SINGLETON->receiverType
           else->throw RuntimeException("不存在此创建模式")
        }

        return FieldSpec.builder(fieldType,"instance")
            .addModifiers(Modifier.PRIVATE)
            .addModifiers(Modifier.STATIC)
            .build()

    }

    private fun getMethod(busAwareInfo: BusAwareInfo,receiverType: ClassName):MethodSpec{
        val method = MethodSpec.methodBuilder("get")
            .addModifiers(Modifier.STATIC,Modifier.PUBLIC)
            .returns(receiverType)
        val runtimeType = ClassName.get(RuntimeException::class.java)
        if(busAwareInfo.service == null){
           return method
               .beginControlFlow("if(instance == null)")
               .addStatement("throw new \$T(\$S)",runtimeType,"不是@Service不会自动创建对象")
               .endControlFlow()
               .addStatement("\$T receiver= instance.get()",receiverType)
               .beginControlFlow("if(receiver==null)")
               .addStatement("throw new \$T(\$S)",runtimeType,"对象已经被回收")
               .endControlFlow()
               .addStatement("return receiver").build()
        }

        val ifNull = CodeBlock.builder().beginControlFlow("if(receiver == null)")
            .addStatement("receiver = create()")
            .endControlFlow().build()
        when(busAwareInfo.createStrategy){
            CREATE_PER->method.addStatement("\$T receiver = create()",receiverType)
            CREATE_SCOPE->method
                .addStatement("\$T receiver = null",receiverType)
                .beginControlFlow("if(instance == null)")
                .addStatement("receiver = create()")
                .addStatement("return receiver")
                .endControlFlow()
                .addStatement("receiver = instance.get()",receiverType)
                .addCode(ifNull)
            CREATE_SINGLETON->method.addStatement("\$T receiver = instance",receiverType)
                .addCode(ifNull)
        }
        return method.addStatement("return receiver").build()
    }

    private fun setInstance(name:String,busAwareInfo: BusAwareInfo,receiverType: ClassName):CodeBlock{
        val weakReferenceType =ParameterizedTypeName.get(ClassName.get(WeakReference::class.java),receiverType)
        return when(busAwareInfo.createStrategy){
            CREATE_PER, CREATE_SCOPE->CodeBlock.builder().addStatement("instance = new \$T($name)",weakReferenceType).build()
            CREATE_SINGLETON->CodeBlock.builder().addStatement("instance = $name").build()
            else->throw RuntimeException("不能识别的模式${busAwareInfo.createStrategy}")
        }
    }

    private fun injectMethod(instanceType:ClassName,busAwareInfo: BusAwareInfo):MethodSpec{
        val method = MethodSpec.methodBuilder("inject")
            .addModifiers(Modifier.PUBLIC,Modifier.STATIC)
            .addParameter(instanceType,"receiver")
            .addCode(setInstance("receiver",busAwareInfo,instanceType))
            .addStatement("autoWire(receiver)")
        if(busAwareInfo.busEvent.isNotEmpty()){
            method.addStatement("registerEvents(receiver)")
        }
        return method.build()
    }

    private fun provideMethod(busAwareInfo: BusAwareInfo,provideInfo: ProvideInfo):MethodSpec{
        return MethodSpec.methodBuilder(provideInfo.functionName)
            .addModifiers(Modifier.PUBLIC,Modifier.STATIC)
            .returns(Utils.getClassName(provideInfo.returnType))
            .addStatement("return get().${provideInfo.functionName}()")
            .build()
    }

    private fun creatorHandler(info: BusAwareInfo) {

        //创建接收者的方法 create
        val instanceType = Utils.getClassName(info.receiverClass)
        val typeName = "${instanceType.simpleName()}$CREATOR"

        val type = TypeSpec.classBuilder(typeName)
            .addField(getReceiverField(info,instanceType))
            .addModifiers(Modifier.PUBLIC)
            .addMethod(autoWireMethod(info,instanceType))
            .addMethod(getMethod(info,instanceType))
            .addMethod(injectMethod(instanceType,busAwareInfo = info))

          for(provide in info.provideInfo){
              type.addMethod(provideMethod(info,provide))
          }
        if(info.service != null){
            type.addMethod(createIsViewModelMethod(info))
        }
        if(info.busEvent.isNotEmpty()){
            type.addMethod(registerEventMethod(info,instanceType))
        }
        if(info.registerInfo.isNotEmpty()){
            type.addStaticBlock(registerMethod(info))
        }
        try {
            JavaFile.builder("$BASE_PACKAGE.$moduleName", type.build()).build()
                .writeTo(filer)
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }


    private fun fetchServiceInfo(e: Element,viewOwnerFrom:String? = null) {
        val createModel = e.getAnnotation(Service::class.java)?.createModel?: CREATE_SCOPE
        val type = if (e is TypeElement) {
            e
        } else {
            (e.enclosingElement as TypeElement)
        }
        val typeName = type.qualifiedName.toString()
        val key = ArrayList<String>()
        key.add(type.superclass.toString())
        key.addAll(type.interfaces.map { it.toString() })
        for(k in key){
            staticInfo[k] = StaticInfo(typeName,"get")
        }
        val info = busAwareInfo(typeName)
        val argsSignature = Utils.getSignature(e)
        info.service = ServiceInfo(argsSignature,viewOwnerFrom)
        info.createStrategy = createModel

    }




    private fun fetchFunctionInfo(e: ExecutableElement) {
        val thread = e.getAnnotation(BusEvent::class.java).threadPolicy
        val argsSignature = Utils.getSignature(e)
        val isStick = e.getAnnotation(BusEvent::class.java).stick
        val info = busAwareInfo(e.enclosingElement.toString())
        info.busEvent.add(BusEventInfo(e.simpleName.toString(), argsSignature, thread,isStick))
    }

    private fun busAwareInfo(key: String): BusAwareInfo {
        return if (busInfos.containsKey(key)) {
            busInfos[key]!!
        } else {
            val info = BusAwareInfo(key, ArrayList(), null, ArrayList(), ArrayList(),ArrayList())
            busInfos[key] = info
            info
        }
    }

}