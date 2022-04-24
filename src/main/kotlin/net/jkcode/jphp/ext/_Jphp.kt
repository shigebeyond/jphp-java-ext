package net.jkcode.jphp.ext

import net.jkcode.jkutil.common.getAccessibleField
import php.runtime.Memory
import php.runtime.invoke.Invoker
import php.runtime.lang.IObject
import php.runtime.memory.*

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

val mapField = ArrayMemory::class.java.getAccessibleField("map")!!
val listField = ArrayMemory::class.java.getAccessibleField("_list")!!

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
        is ArrayMemory -> if(this.isMap()) mapField.get(this) else listField.get(this)
        is ObjectMemory -> this.value
        is ReferenceMemory -> this.value.toJavaObject() // 递归
        else -> throw IllegalArgumentException("Cannot auto convert [$this] into JavaObject")
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