package net.jkcode.jphp.ext

import php.runtime.Memory
import php.runtime.memory.ObjectMemory
import java.util.concurrent.CompletableFuture

/**
 * 扩展 CompletableFuture，用来标识纯php方法返回值类型是 WrapCompletableFuture 的情况
 *    使用 MethodGuardInvoker.invokeAfterGuard() 代理调用方法, 会对java/php方法的调用结果都统一封装为 CompletableFuture 结果(详见 HttpRequestHandler), 以方便后续guard处理
 *    但代理调用处, 还是要将 CompletableFuture 结果转为 方法返回值类型, java方法可以很轻松的获得返回值类型, 但php方法不行, 因此使用 PhpCompletableFuture 扩展类来标识php方法返回值类型是 WrapCompletableFuture 的情况
 * @author shijianhang<772910474@qq.com>
 * @date 2022-4-27 7:25 PM
 */
class PhpReturnCompletableFuture(future: CompletableFuture<Any?>): CompletableFuture<Any?>() {

    init {
        // 包装源future
        future.whenComplete { r, ex ->
            // 设置结果
            if(ex == null)
                this.complete(r)
            else
                this.completeExceptionally(ex)
        }
    }

    companion object{

        /**
         * 对supplier包装try/catch, 并包装与返回异步结果, 兼容supplier结果值是 CompletableFuture 的情况
         *
         * @param supplier 取值函数
         * @return
         */
        public inline fun tryPhpSupplierFuture(supplier: () -> Memory): CompletableFuture<Any?> {
            try{
                // 调用取值函数
                val result = supplier.invoke()

                // 异步结果: 使用 PhpCompletableFuture 扩展类来标识php方法返回值类型是 WrapCompletableFuture 的情况
                if(result is ObjectMemory && result.value is WrapCompletableFuture) {
                    val f = (result.value as WrapCompletableFuture).future
                    return PhpReturnCompletableFuture(f)
                }

                // 同步结果
                return CompletableFuture.completedFuture(result)
            }catch (r: Throwable){
                val result2 = CompletableFuture<Any?>()
                result2.completeExceptionally(r)
                return result2
            }
        }
    }
}