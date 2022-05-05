package net.jkcode.jphp.ext

import net.jkcode.jkguard.Map2AnnotationHandler
import net.jkcode.jkutil.common.associate
import php.runtime.Memory
import php.runtime.invoke.Invoker
import php.runtime.lang.ForeachIterator
import php.runtime.lang.IObject
import php.runtime.memory.*
import php.runtime.reflection.ClassEntity
import php.runtime.reflection.MethodEntity
import java.util.*

/**
 * 将java对象转换为jphp的Memory
 * @return
 */
public inline fun Any?.toMemory(): Memory {
    return when(this) {
        null -> NullMemory.INSTANCE
        is Boolean -> if(this) TrueMemory.INSTANCE else FalseMemory.INSTANCE
        is Int -> LongMemory(this.toLong())
        is Long -> LongMemory(this)
        is Short -> LongMemory(this.toLong())
        is Byte -> LongMemory(this.toLong())
        is Float -> DoubleMemory(this.toDouble())
        is Double -> DoubleMemory(this)
        is String -> StringMemory(this)
        is String -> StringMemory(this)
        is List<*> -> ArrayMemory(this)
        is Map<*, *> -> ArrayMemory(this)
        is IObject -> ObjectMemory(this)
        is WrapJavaObject -> ObjectMemory(this)
        else -> throw IllegalArgumentException("Cannot auto convert [$this] into Memory")
    }
}

/**
 * 将jphp的Memory转换为java对象
 * @return
 */
public fun Memory.toJavaObject(): Any? {
    return when(this) {
        is NullMemory -> null
        is TrueMemory -> true
        is FalseMemory -> false
        is LongMemory -> this.value
        is DoubleMemory -> this.toDouble()
        is StringMemory -> this.toString()
        is ArrayMemory -> if(this.isMap()) this.toPureMap() else this.toPureArray() // 递归调用
        is ObjectMemory -> this.value
        is ReferenceMemory -> this.value.toJavaObject() // 递归
        else -> throw IllegalArgumentException("Cannot auto convert [$this] into JavaObject")
    }
}

/**
 * ArrayMemory 转纯粹的java map
 */
fun ArrayMemory.toPureMap(): Map<String, Any?> {
    val r: MutableMap<String, Any?> = LinkedHashMap()
    val iterator: ForeachIterator = foreachIterator(false, false)
    while (iterator.next()) {
        val key = iterator.key.toString()
        val value = iterator.value.toJavaObject() // 递归调用
        r[key] = value
    }
    return r
}

/**
 * ArrayMemory 转纯粹的java list
 */
fun ArrayMemory.toPureArray(): List<Any?> {
    return this.map {
        it.toJavaObject() // 递归调用
    }
}

/**
 * 根据参数来确定是二选一调用
 *   1 call(Memory): 参数是php对象
 *   2 callAny(Any): 参数是java对象
 * @param args
 * @return
 */
public fun Invoker.callMemoryOrAny(vararg args: Any?): Memory? {
    // 1 call(Memory): 参数是php对象
    if(args.isEmpty())
        return this.call()
    if(args[0] is Memory) {
        val args2 = args as Array<Memory>
        return this.call(*args2)
    }

    // 2 callAny(Any): 参数是java对象
    return this.callAny(args)
}

/**
 * 某个php类的所有php方法的java注解
 *   要缓存到 ClassEntity 中
 */
public val ClassEntity.methodAnnotations: Map<String, Map<Class<*>, Any>>
    get(){
        return this.getAdditionalData("methodAnnotations", Map::class.java){
            // 注解配置： {方法名:{注解类名:{注解属性}}}, 其中 注解类名:{注解属性} 如 "net.jkcode.jkguard.annotation.GroupCombine":{"batchMethod":"listUsersByNameAsync","reqArgField":"name","respField":"","one2one":"true","flushQuota":"100","flushTimeoutMillis":"100"}
            val annProp = this.findStaticProperty("_methodAnnotations")
            val annConfigs = annProp.getStaticValue(JphpLauncher.environment, null) as ArrayMemory
            // 构建注解
            annConfigs.toPureMap().associate { methodName, annConfig ->
                methodName to buildMethodAnnotation(annConfig as Map<String, Map<String, Any?>>)
            }
        } as Map<String, Map<Class<*>, Any>>
    }

/**
 * 构建单个php方法的注解
 * @param annConfig 注解配置： {注解类名:{注解属性}}，如 "net.jkcode.jkguard.annotation.GroupCombine":{"batchMethod":"listUsersByNameAsync","reqArgField":"name","respField":"","one2one":"true","flushQuota":"100","flushTimeoutMillis":"100"}
 * @return Map<注解类, 注解实例>
 */
private fun buildMethodAnnotation(annConfig: Map<String, Map<String, Any?>>): Map<Class<*>, Any> {
    return annConfig.associate{ clazzName, attrs ->
        val clazz = Class.forName(clazzName) // 注解类
        val ann = Map2AnnotationHandler.map2Annotation(clazz, attrs) // json转注解实例
        clazz to ann
    }
}

/**
 * 获得php方法的注解
 */
public val MethodEntity.annotations: Map<Class<*>, Any>
    get(){
        return this.clazz.methodAnnotations[this.name] ?: emptyMap()
    }

/**
 * 获得属性值
 */
public fun ObjectMemory.getPropValue(name: String): Memory? {
    val classEntity: ClassEntity = this.value.reflection
    val prop = classEntity.findProperty(name)
    return prop.getValue(JphpLauncher.environment, null, this.value)
}

/**
 * 获得属性java值
 */
public fun ObjectMemory.getPropJavaValue(name: String): Any? {
    return getPropValue(name)?.toJavaObject()
}