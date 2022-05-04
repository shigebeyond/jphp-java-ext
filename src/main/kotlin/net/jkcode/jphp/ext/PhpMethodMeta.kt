package net.jkcode.jphp.ext

import co.paralleluniverse.fibers.Suspendable
import net.jkcode.jkguard.IMethodGuardInvoker
import net.jkcode.jkguard.IMethodMeta
import php.runtime.Memory
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
        public override val handler: IMethodGuardInvoker // 带守护的方法调用者
): IMethodMeta {

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
     *
     * @param resFuture
     * @return
     */
    @Suspendable
    override fun getResultFromFuture(resFuture: CompletableFuture<*>): Any?{
        return method.getResultFromFuture(resFuture)
    }

    /**
     * 获得兄弟方法
     * @param name 兄弟方法名
     * @return
     */
    override fun getBrotherMethod(name: String): IMethodMeta{
        val brotherMethod = method.clazz.findMethod(name)
        return PhpMethodMeta(brotherMethod, handler)
    }

    /**
     * 方法处理
     *   在IMethodGuardInvoker#invokeAfterGuard()中调用
     *   实现：server端实现是调用包装的原生方法, client端实现是发rpc请求
     */
    @Suspendable
    override fun invoke(obj: Any, vararg args: Any?): Any? {
        return method.invokeDynamic((obj as ObjectMemory).value, JphpLauncher.instance().environment, null, *(args as Array<Memory>))
    }
}