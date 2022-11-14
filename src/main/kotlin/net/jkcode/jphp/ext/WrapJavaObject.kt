package net.jkcode.jphp.ext

import net.jkcode.jkutil.common.getMethodByName
import php.runtime.Memory
import php.runtime.annotation.Reflection
import php.runtime.env.Environment
import php.runtime.ext.java.JavaClass
import php.runtime.ext.java.JavaMethod
import php.runtime.ext.java.JavaObject
import php.runtime.ext.java.JavaReflection
import php.runtime.lang.BaseWrapper
import php.runtime.memory.ObjectMemory
import php.runtime.memory.StringMemory
import php.runtime.memory.support.MemoryUtils
import php.runtime.reflection.ClassEntity

/**
 * 包装java对象，用来读写属性+动态调用方法
 * 1 动态调用方法的实现
 *    仿jphp自带的 JavaObject，但该类并不能动态调用方法
 *    动态调用方法的实现，使用魔术方法，使用`JavaMethod.invokeArgs()`来负责参数类型+返回值类型转换
 * 2 属性读写先调动getter/setter
 * 3 使用
 *    java中的实例化： val obj = WrapJavaObject.of(env, xxx)
 *    php中的实例化: $obj = new WrapJavaObject($xxx);
 *    php中的方法调用（默认方法）: $obj->yyy();
 */
@Reflection.Name("php\\lang\\WrapJavaObject")
open class WrapJavaObject(env: Environment, clazz: ClassEntity) : BaseWrapper<JavaObject>(env, clazz) {

    // 被包装的java对象
    lateinit var obj: Any

    @Reflection.Signature
    fun __construct(obj: Any): Memory {
        this.obj = obj
        return Memory.NULL
    }

    //__call()实现一： wrong, 不严谨，因为jphp自动转换的实参类型（如数字会转为Long），可能对不上方法的形参类型(如int)
    /*@Reflection.Signature
    fun __call(name: String, vararg args: Any?): Any? {
        try {
            // 获得方法
            val method = obj.javaClass.getMethodByName(name)
            if(method == null)
                throw NoSuchMethodException("类[${obj.javaClass}]无方法[$name]")
            // 调用方法
            return method.invoke(obj, *args)
        } catch (e: Exception) {
            JavaReflection.exception(env, e)
        }
        return Memory.NULL
    }*/

    //__call()实现二： 使用 JavaMethod 包装方法调用
    @Reflection.Signature(value = [Reflection.Arg("name"), Reflection.Arg("arguments")])
    fun __call(env: Environment, vararg args: Memory): Memory {
        try {
            // 第一个参数是方法名
            val name = args[0].toString()
            // 获得方法
            val method = obj.javaClass.getMethodByName(name)
            if(method == null)
                throw NoSuchMethodException("类[${obj.javaClass}]无方法[$name]")
            // 用 JavaMethod 包装方法调用
            val method2 = JavaMethod.of(env, method)
            val args2 = args.toMutableList()
            args2[0] = ObjectMemory(JavaObject.of(env, obj)) // 第一个参数，原来是方法名，现替换为被包装的java对象
            return method2.invokeArgs(env, *args2.toTypedArray())
        } catch (e: Exception) {
            JavaReflection.exception(env, e)
        }
        return Memory.NULL
    }

    // ---------------- 改进final类JavaObject实现，属性读写先调动getter/setter ---------------
    @Reflection.Signature(Reflection.Arg("name"))
    fun __get(env: Environment, vararg args: Memory): Memory {
        val name = args[0].toString()

        // 1 先尝试调用getter
        val v = call_getter(env, obj, name)
        if(v != null)
            return v

        // 2 再读字段
        try {
            val field = obj.javaClass.getField(name)
            field.isAccessible = true
            return MemoryUtils.valueOf(env, field[obj])
        } catch (e: NoSuchFieldException) {
            JavaReflection.exception(env, e)
        } catch (e: IllegalAccessException) {
            JavaReflection.exception(env, e)
        }
        return Memory.NULL
    }

    @Reflection.Signature(value = [Reflection.Arg("name"), Reflection.Arg("value")])
    fun __set(env: Environment, vararg args: Memory): Memory {
        val name = args[0].toString()
        // 1 先尝试调用setter
        val v = call_setter(env, obj, name, args[1])
        if(v != null)
            return v

        // 2 再写字段
        try {
            val field = obj.javaClass.getField(name)
            field.isAccessible = true
            field[obj] = MemoryUtils.fromMemory(args[1], field.type)
        } catch (e: NoSuchFieldException) {
            JavaReflection.exception(env, e)
        } catch (e: IllegalAccessException) {
            JavaReflection.exception(env, e)
        }
        return Memory.NULL
    }

    @Reflection.Name("getClass")
    @Reflection.Signature
    fun _getClass(env: Environment, vararg args: Memory): Memory {
        return ObjectMemory(JavaClass.of(env, obj.javaClass))
    }

    @Reflection.Signature
    fun getClassName(env: Environment, vararg args: Memory): Memory {
        return StringMemory(obj.javaClass.name)
    }

    companion object {
        // 创建 WrapJavaObject 实例
        fun of(env: Environment, value: Any): WrapJavaObject {
            val javaObject = WrapJavaObject(env, env.fetchClass("php\\lang\\WrapJavaObject"))
            javaObject.obj = value
            return javaObject
        }
    }
}