package net.jkcode.jphp.ext

import php.runtime.Memory
import php.runtime.annotation.Reflection
import php.runtime.env.Environment
import php.runtime.invoke.Invoker
import php.runtime.lang.BaseWrapper
import php.runtime.memory.support.MemoryUtils
import php.runtime.reflection.ClassEntity
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit

/**
 * 包装 CompletableFuture 对象
 *    1 回调中使用 Invoker.callAny() 来将回调的java参数变为php参数
 *    2 CompletableFuture.result 可能是java对象，因此get()需要将java对象转为php对象
 *
 */
@Reflection.Name("php\\lang\\CompletableFuture")
class PCompletableFuture: BaseWrapper<CompletableFuture<Any?>> {

    constructor(env: Environment, wrappedObject: CompletableFuture<Any?>) : super(env, wrappedObject) {}

    constructor(env: Environment, wrappedObject: PhpReturnCompletableFuture) : super(env, wrappedObject) {}
    
    constructor(env: Environment, clazz: ClassEntity) : super(env, clazz) {}

    public val future: CompletableFuture<Any?>
        get() = __wrappedObject

    // ---------------------- Future 方法扩展: 参考 WrapFuture ----------------------
    @Reflection.Signature
    private fun __construct(env: Environment, vararg args: Memory): Memory {
        return Memory.NULL
    }

    @Reflection.Signature
    fun isCancelled(env: Environment, vararg args: Memory): Memory {
        return if (future.isCancelled) Memory.TRUE else Memory.FALSE
    }

    @Reflection.Signature
    fun isDone(env: Environment, vararg args: Memory): Memory {
        return if (future.isDone) Memory.TRUE else Memory.FALSE
    }

    @Reflection.Signature(Reflection.Arg("mayInterruptIfRunning"))
    fun cancel(env: Environment, vararg args: Memory): Memory {
        return if (future.cancel(args[0].toBoolean())) Memory.TRUE else Memory.FALSE
    }

    @Reflection.Signature(Reflection.Arg(value = "timeout", optional = Reflection.Optional("NULL")))
    fun get(env: Environment, vararg args: Memory): Memory {
        // 获得 CompletableFuture.result
        val v = if (args[0].isNull)
                    future.get()
                else
                    future.get(args[0].toLong(), TimeUnit.MILLISECONDS) // 有超时

        // CompletableFuture.result 可能是java对象，因此需要将java对象转为php对象
        if(v == null)
            return Memory.NULL
        //return MemoryUtils.valueOf(env, v) // 有env参数，居然不转map/list
        return MemoryUtils.valueOf(v)
    }

    // ---------------------- CompletableFuture 方法扩展 ----------------------
    @Reflection.Signature
    fun thenApply(env: Environment, invoker: Invoker): PCompletableFuture {
        val f = future.thenApply { v ->
            invoker.callMemoryOrAny(v)
        }
        return PCompletableFuture(env, f as CompletableFuture<Any?>)
    }

    @Reflection.Signature
    fun exceptionally(env: Environment, invoker: Invoker): PCompletableFuture {
        val f = future.exceptionally { ex ->
            invoker.callMemoryOrAny(ex) as Nothing?
        }
        return PCompletableFuture(env, f)
    }

    @Reflection.Signature
    fun whenComplete(env: Environment, invoker: Invoker): PCompletableFuture {
        val f = future.whenComplete { r, ex ->
            invoker.callMemoryOrAny(r, ex)
        }
        return PCompletableFuture(env, f)
    }

}
