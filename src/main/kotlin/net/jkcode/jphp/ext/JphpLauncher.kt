package net.jkcode.jphp.ext

import net.jkcode.jkutil.common.getAccessibleField
import php.runtime.Memory
import php.runtime.env.CallStackItem
import php.runtime.env.Environment
import php.runtime.env.TraceInfo
import php.runtime.env.handler.ExceptionHandler
import php.runtime.ext.core.OutputFunctions
import php.runtime.ext.core.classes.WrapClassLoader
import php.runtime.ext.core.classes.WrapClassLoader.WrapLauncherClassLoader
import php.runtime.ext.java.JavaObject
import php.runtime.launcher.LaunchException
import php.runtime.launcher.Launcher
import php.runtime.memory.ArrayMemory
import php.runtime.memory.ObjectMemory
import php.runtime.memory.support.MemoryOperation
import php.runtime.memory.support.MemoryUtils
import php.runtime.reflection.support.ReflectionUtils
import java.io.IOException
import java.io.OutputStream
import java.util.concurrent.CompletableFuture

/**
 * jphp启动器
 *   1. 支持执行指定php文件，并输出到指定流
 *   2. 注册类 PJavaObject、PCompletableFuture
 *   3. 初始通用的转换器
 *   4. 多线程支持：environment默认是 ConcurrentEnvironment，支持多线程调用
 */
object JphpLauncher : Launcher() {

    // ---- 转换器 ----
    // Memory -> java object的转换器: 用在php中调用java方法，在 PJavaObject / WrapReferer 等多个扩展类中用到但又不能写到某个扩展类中初始化(重复或漏)，因此统一写到这里来初始化
    public val CONVERTERS: HashMap<Class<*>, MemoryUtils.Converter<*>> = MemoryUtils::class.java.getAccessibleField("CONVERTERS")!!.get(null) as HashMap<Class<*>, MemoryUtils.Converter<*>>
    // java object -> Memory 的反转器: 用在将java对象暴露给php
    public val UNCONVERTERS: HashMap<Class<*>, MemoryUtils.Unconverter<*>> = MemoryUtils::class.java.getAccessibleField("UNCONVERTERS")!!.get(null) as HashMap<Class<*>, MemoryUtils.Unconverter<*>>
    private fun initConverters(){
        // 1 Memory -> java object
        /* 下面的 value.toJavaObject() 已包含List/Map等转换
        // 1.1 添加 List 类型转换器
        CONVERTERS.put(List::class.java, object : MemoryUtils.Converter<List<*>>() {
            override fun run(env: Environment?, trace: TraceInfo?, value: Memory): List<*> {
                // 检查是否数组
                if (value !is ArrayMemory)
                    throw IllegalArgumentException("Cannot convert php object [$value] to java List")

                // 逐个元素转java对象
                return value.toPureList()
            }
        })

        // 1.2 添加 Map 类型转换器
        CONVERTERS.put(Map::class.java, object : MemoryUtils.Converter<Map<*, *>>() {
            override fun run(env: Environment?, trace: TraceInfo?, value: Memory): Map<*, *> {
                // 检查是否数组
                if (value !is ArrayMemory)
                    throw IllegalArgumentException("Cannot convert php object [$value] to java Map")

                return value.toPureMap()
            }
        })*/

        // 1.3 添加默认 object 类型的转换器，否则由于找不到 object 类型的转换器导致直接将实参值转换为null, 如 Hashmap 的 put(Object key, Object value) 方法, 在php调用 map.put('price', 11)时到java就变成 map.put(null, null)
        CONVERTERS.put(Any::class.java, object : MemoryUtils.Converter<Any?>() {
            override fun run(env: Environment?, trace: TraceInfo?, value: Memory): Any? {
                return value.toJavaObject() // 转java对象, 包含List/Map等转换
            }
        })

        // 2 java object -> Memory
        // 2.1 添加 Completable 类型的反转器
        UNCONVERTERS.put(CompletableFuture::class.java, object : MemoryUtils.Unconverter<CompletableFuture<Any?>> {
            override fun run(value: CompletableFuture<Any?>): Memory {
                return ObjectMemory(PCompletableFuture(environment, value))
            }
        })
    }

    init {
        // 注册java对象， 方便调用java对象
        val core = compileScope.getExtension("Core")
        compileScope.registerLazyClass(core, JavaObject::class.java)
        compileScope.registerLazyClass(core, PJavaObject::class.java)
        compileScope.registerLazyClass(core, PLog::class.java)
        compileScope.registerLazyClass(core, PCache::class.java)
        compileScope.registerLazyClass(core, PCompletableFuture::class.java)
        // bug: php.runtime.reflection.CompileMethodEntity$CompileMethod$Method.setParameters(CompileMethodEntity.java:314) 报错 Unsupported type for binding - class java.util.concurrent.CompletableFuture in net.jkcode.jkmvc.http.jphp.PHttpRequest.transferAndReturn
        // 原因: MemoryOperation 转换器中没包含 CompletableFuture 类型转换
        // 解决: 参考实现 extend.registerWrapperClass(scope, CompletableFuture::class.java, PCompletableFuture::class.java)
         MemoryOperation.registerWrapper(CompletableFuture::class.java, PCompletableFuture::class.java);
         MemoryOperation.registerWrapper(PhpReturnCompletableFuture::class.java, PCompletableFuture::class.java);
        // 注册 JkHashMapMemoryOperation, 以便替代 HashMapMemoryOperation (HashMapMemoryOperation由于没有指定key/value泛型, 导致转换时报错ArrayIndexOutOfBoundsException)
        MemoryOperation.register(JkHashMapMemoryOperation())

        // 配置
        readConfig()

        // 初始化扩展: 会初始化 environment
        initExtensions()

        // 修改 Environment.moduleManager 为 JkModuleManager, 以便支持在卸载模块时也卸载模块的类/函数/常量
        // moduleManager是保护属性, 用反射来写
        //environment.moduleManager = JkModuleManager(environment)
        moduleManagerField.set(environment, JkModuleManager(environment))

        if (isDebug()) {
            if (compileScope.tickHandler == null) {
                throw LaunchException("Cannot find a debugger, please add the jphp-debugger dependency")
            }
        }

        // 类加载器
        val classLoader = config.getProperty("env.classLoader", ReflectionUtils.getClassName(WrapLauncherClassLoader::class.java))
        val classLoaderEntity = environment.fetchClass(classLoader)
        val loader = classLoaderEntity.newObject<WrapClassLoader>(environment, TraceInfo.UNKNOWN, true)
        environment.invokeMethod(loader, "register", Memory.TRUE)

        // 初始转换器
        initConverters()
    }

    /**
     * 执行指定php文件，并输出到指定流
     *    php执行结果有可能是PCompletableFuture, 直接返回future, 以便调用端处理异步结果
     *
     * @param bootstrapFile php入口文件
     * @param args 参数
     * @param out 输出流
     * @param outputBuffering 是否打开缓冲区
     * @param exceptionHandler 一次性异常处理器，该php文件执行后会恢复原来的异常处理器
     * @return
     */
    fun run(bootstrapFile: String, args: Map<String, Any?> = emptyMap(), out: OutputStream? = null, outputBuffering: Boolean = true, exceptionHandler: ExceptionHandler? = null): Any? {
        // 加载入口文件
        val bootstrap = loadFrom(bootstrapFile) ?: throw IOException("Cannot find '$bootstrapFile' resource")

        // 前置处理
//        beforeIncludeBootstrap()

        // 显示字节码
        /*if (StringMemory(config.getProperty("bootstrap.showBytecode", "")).toBoolean()) {
            val moduleOpcodePrinter = ModuleOpcodePrinter(bootstrap)
            println(moduleOpcodePrinter.toString())
        }*/

        // 添加全局参数
        /*
        val argv = ArrayMemory.ofStrings(*this.args)
        val path = URLDecoder.decode(
                Launcher::class.java.protectionDomain.codeSource.location.toURI().path,
                "UTF-8"
        )
        argv.unshift(StringMemory.valueOf(path))
        environment.globals.put("argv", argv)
        environment.globals.put("argc", LongMemory.valueOf(argv.size()))
        */

        // 添加本地参数
        val locals = ArrayMemory(true)
        for ((k, v) in args) {
            locals.put(k, v?.toMemory())
        }

        // 调用入栈
        val stackItem = CallStackItem(bootstrap.trace)
        environment.pushCall(stackItem)

        // 设置输出流
        if(out != null)
            environment.outputBuffers.peek().output = out

        // 打开缓冲区， 等价于php ob_start()
        if(outputBuffering)
            OutputFunctions.ob_start(environment, bootstrap.trace)

        // 异常处理
        val oeh = environment.exceptionHandler // 原来的异常处理器
        if(exceptionHandler != null) //一次性异常处理器，该php文件执行后会恢复原来的异常处理器
            environment.exceptionHandler = exceptionHandler

        try {
            // include 执行
            val ret = bootstrap.include(environment, locals)?.toJavaObject()
            if (ret is PCompletableFuture) // 返回值有可能是future包装器, 直接返回future, 以便调用端处理异步结果
                return ret.future
            return ret
        } finally {
            // 恢复原来的异常处理器
            if(exceptionHandler != null)
                environment.exceptionHandler = oeh

            // 发送内部缓冲区的内容到浏览器，并且关闭输出缓冲区
            if(outputBuffering)
                OutputFunctions.ob_end_flush(environment, bootstrap.trace)

            // 后置处理
//            afterIncludeBootstrap()

            // 调用出栈
            environment.popCall()

            // 进程结束的回调
//            compileScope.triggerProgramShutdown(environment)

            // 清理gc对象 + OutputBuffer
            environment.doFinal()
        }
    }

}