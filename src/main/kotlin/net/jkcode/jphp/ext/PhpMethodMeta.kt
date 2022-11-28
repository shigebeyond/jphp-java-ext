package net.jkcode.jphp.ext

import co.paralleluniverse.fibers.Suspendable
import net.jkcode.jkguard.IMethodGuardInvoker
import net.jkcode.jkguard.IMethodMeta
import net.jkcode.jkutil.common.commonLogger
import net.jkcode.jkutil.common.errorColor
import net.jkcode.jkutil.fiber.AsyncCompletionStage
import php.runtime.Memory
import php.runtime.env.CallStackItem
import php.runtime.env.TraceInfo
import php.runtime.ext.java.JavaException
import php.runtime.memory.ObjectMemory
import php.runtime.reflection.MethodEntity
import java.util.concurrent.CompletableFuture

/**
 * 基于php Method实现的方法元数据
 *   基本上就是代理Method，为了兼容php方法，才抽取的IMethodMeta
 *
 * @author shijianhang<772910474@qq.com>
 * @date 2022-4-27 7:25 PM
 */
class PhpMethodMeta(
        protected val method: MethodEntity, // php方法
        handler: IMethodGuardInvoker // 带守护的方法调用者
): IMethodMeta<Memory>(handler) {

    /**
     * 类名
     */
    override val clazzName: String
        get() = method.clazz.name

    /**
     * 方法名
     */
    override val methodName: String
        get() = method.name

    /**
     * 方法签名(rpc用到), 因此此类无用
     */
    override val methodSignature: String
        get() = method.name

    /**
     * 方法参数类型
     *    会在 degradeHandler/groupCombiner/keyCombiner 用来检查方法的参数与返回值类型
     */
    override val parameterTypes: Array<Class<*>>
        get() = throw UnsupportedOperationException("纯php方法无精确参数类型")

    /**
     * 返回值类型
     */
    override val returnType: Class<*>
        get() = throw UnsupportedOperationException("纯php方法无精确返回值类型")

    /**
     * 是否纯php实现
     *    用来决定是否在 degradeHandler/groupCombiner/keyCombiner 用来检查方法的参数与返回值类型
     */
    override val isPurePhp: Boolean
        get() = true

    /**
     * 获得方法注解
     * @param annotationClass 注解类
     * @return
     */
    override fun <A : Annotation> getAnnotation(annotationClass: Class<A>): A? {
        return method.annotations[annotationClass] as A?
    }

    /**
     * 从CompletableFuture获得方法结果值
     *    1 区别于java方法实现(MethodMeta.getResultFromFuture()): 根据java方法返回类型(是否CompletableFuture)来决定返回异步or同步结果
     *      php方法是无返回类型, 只能运行时根据结果的实际类型(是否CompletableFuture)来决定返回异步or同步结果
     *    2 同步结果值有可能是null或java对象
     * @param resFuture
     * @return
     */
    @Suspendable
    override fun getResultFromFuture(resFuture: CompletableFuture<*>): Any?{
        // 1 异步结果
        // 由于php方法无法获得返回值类型, 因此使用 PhpCompletableFuture 扩展类来标识php方法返回值类型是 PCompletableFuture 的情况
//        if(returnType == Future::class.java || returnType == CompletableFuture::class.java)
        if(resFuture is PhpReturnCompletableFuture)
            return resFuture

        // 2 同步结果
        //return resFuture.get()
        return AsyncCompletionStage.get(resFuture) // 支持协程
    }

    /**
     * 获得兄弟方法, 用在获得降级或合并的兄弟方法
     * @param name 兄弟方法名
     * @return
     */
    override fun getBrotherMethod(name: String): IMethodMeta<Memory> {
        val brotherMethod = method.clazz.findMethod(name.toLowerCase())
        return PhpMethodMeta(brotherMethod, handler)
    }

    /**
     * php方法调用
     *   在server端的IMethodGuardInvoker#invokeAfterGuard()/两端的降级处理中调用
     *   实现：server端实现是调用包装的本地方法, client端实现是发rpc请求
     * @param obj php对象
     * @param args php参数
     * @return Memory
     */
    @Suspendable
    override fun invoke(obj: Any, vararg args: Any?): Memory {
        val env = JphpLauncher.environment
        // 1 调用入栈, 否则无法丢失当前类, 会导致无法调用父类私有方法的bug
        val stackItem = buildStackItem(obj, args)
        env.pushCall(stackItem)
        // 2 调用php方法
        try {
            return method.invokeDynamic((obj as ObjectMemory).value, env, null, *(args as Array<Memory>))
        }finally {
            // 3 调用出栈
            env.popCall()
        }
    }

    /**
     * 构建调用栈项
     */
    private fun buildStackItem(obj: Any, args: Array<out Any?>): CallStackItem {
        val clazz = this.method.clazz
        val trace = TraceInfo(clazz.module.name, 0, 0)
        val stackItem = CallStackItem(trace, (obj as ObjectMemory).value, args as Array<Memory>, method.name, clazz.name, clazz.name)
        stackItem.staticClassEntity = clazz
        stackItem.classEntity = clazz
        return stackItem
    }

    /**
     * 对invoke()包装try/catch, 并包装与返回异步结果, 兼容invoke()结果值是 CompletableFuture 的情况
     *
     * @param obj php对象
     * @param args php参数
     * @return
     */
    public inline fun tryInvokeFuture(obj: Any, vararg args: Any?): CompletableFuture<Any?> {
        try{
            // 调用php方法
            val result = invoke(obj, *args)

            // 异步结果: 使用 PhpCompletableFuture 扩展类来标识php方法返回值类型是 PCompletableFuture 的情况, 用在 PhpMethodMeta.getResultFromFuture() 根据结果值类型来决定返回异步or同步结果
            if(result is ObjectMemory && result.value is PCompletableFuture) {
                val f = (result.value as PCompletableFuture).future
                return PhpReturnCompletableFuture(f)
            }

            // 同步结果
            return CompletableFuture.completedFuture(result)
        }catch (r: Throwable){
            // 如果是php包装的异常, 则立即打印java原生异常, 否则到http请求处理层时就丢失了java原生的调用栈, 打印了也找不到出错点, 参考<jphp-异常处理.md>
            if(r is JavaException)
                commonLogger.errorColor("调用php方法[$clazzName::$methodName()]时出现java异常如下, 要结合下一个php异常+断点调试来分析", r.throwable)

            // 异常结果
            val result2 = CompletableFuture<Any?>()
            result2.completeExceptionally(r)
            return result2
        }
    }
}