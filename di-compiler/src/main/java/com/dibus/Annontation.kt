package com.dibus

import java.lang.annotation.Inherited
import kotlin.reflect.KClass

const val CREATE_PER = 4 //每次autoWire或者内部调用都会new 一次，实例会用 ReferenceQueue保存起来
const val CREATE_SINGLETON = 5 //优先从已有的实例获取，如果没有，则会从@provide 返回的对象和@Service标识的类中获取
const val CREATE_SCOPE = 6 //强引用保存，除非用户自己创建多个，不然容器内只会存在一个实例


/**
 * 自动创建对象
 * 可以在类和构造函数中使用，能识别它的直接父类和直接接口
 * @param createModel 创建类的模式
 */
@Target(AnnotationTarget.CLASS,AnnotationTarget.CONSTRUCTOR)
@Retention(AnnotationRetention.BINARY)
annotation class Service(val createModel:Int = CREATE_SCOPE)

/**
 * 自动创建viewModel，具备@Service功能，默认createStrategy 为scope
 * @param viewOwner fragment、activity
 */
@Target(AnnotationTarget.CLASS,AnnotationTarget.CONSTRUCTOR)
@Retention(AnnotationRetention.BINARY)
annotation class ViewModelService(val viewOwner:KClass<*>)

/**
 * 自动注入
 * 能注入@Service和@Provide不能同时提供相同的类型，若@Service和@Provide没有提供此类型，则会从
 * <p>dibus中获取，需要调用Dibus.register方法提供
 *
 */
@Target(AnnotationTarget.FIELD,AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.BINARY)
annotation class AutoWire()

/**
 * 具有@Provide和@AutoWire的功能，同时可以指定一个value，value相同才会完成注入
 * DiBus可以调用registerScope完成@Scope中@Provide的功能
 */
@Target(AnnotationTarget.FIELD,AnnotationTarget.FUNCTION,
    AnnotationTarget.CLASS,
    AnnotationTarget.PROPERTY
)
@Inherited
@Retention(AnnotationRetention.BINARY)
annotation class Scope(val value:String = "",val createStrategy:Int = CREATE_SCOPE)

/**
 *public方法使用，接受事件
 * @param threadPolicy 可选
 * @see THREAD_POLICY_MAIN 主线程
 * @see THREAD_POLICY_DEFAULT 发送事件方的线程
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.BINARY)
annotation class BusEvent(val threadPolicy: Int = THREAD_POLICY_DEFAULT,val stick:Boolean = false)


/**
 * 在public方法中使用，如果在@service标识的类中使用，provide会自动生效，不需要提前初始化对象
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.BINARY)
annotation class Provide(val createStrategy:Int = CREATE_SCOPE)

/**
 * 注册一个工厂，可以在dibus和@AutoWire @Scope中获取，建议在同模块中使用@Service和@Provide来替代
 * <p>@Register具备跨模块能力，但是它会被永久注册到DiBus中去
 */
@Target(AnnotationTarget.FUNCTION,AnnotationTarget.CLASS)
@Retention(AnnotationRetention.BINARY)
annotation class Register(val createStrategy:Int = CREATE_SCOPE)

@Target(AnnotationTarget.CLASS)
@Inherited
@Retention(AnnotationRetention.BINARY)
annotation class LifeCycle(val scope:String = "")


const val THREAD_POLICY_MAIN = 1
const val THREAD_POLICY_DEFAULT = 0