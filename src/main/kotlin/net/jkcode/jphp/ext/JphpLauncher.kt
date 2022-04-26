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
import php.runtime.memory.support.MemoryUtils
import php.runtime.reflection.support.ReflectionUtils
import java.io.IOException
import java.io.OutputStream

class JphpLauncher protected constructor() : Launcher() {

    companion object {

        /**
         * 线程独有的可复用的JphpLauncher
         */
        protected val insts: ThreadLocal<JphpLauncher> = ThreadLocal.withInitial {
            JphpLauncher()
        }
        public fun instance(): JphpLauncher {
            return insts.get()
        }

        /**
         * Memory 转 java object的转换器，用在php中调用java方法，在 WrapJavaObject / WrapReferer 等多个扩展类中用到但又不能写到某个扩展类中初始化(重复或漏)，因此统一写到这里来初始化
         */
        public val CONVERTERS: HashMap<Class<*>, MemoryUtils.Converter<*>> = MemoryUtils::class.java.getAccessibleField("CONVERTERS")!!.get(null) as HashMap<Class<*>, MemoryUtils.Converter<*>>
        init {
            // 添加 object 类型的转换器，否则由于找不到 object 类型的转换器导致直接将实参值转换为null, 如 Hashmap 的 put(Object key, Object value) 方法, 在php调用 map.put('price', 11)时到java就变成 map.put(null, null)
            CONVERTERS.put(Any::class.java, object : MemoryUtils.Converter<Any?>() {
                override fun run(env: Environment?, trace: TraceInfo?, value: Memory): Any? {
                    return value.toJavaObject()
                }
            })
        }

    }

    // 只初始化一次
    init {
        // 注册java对象， 方便调用java对象
        val core = compileScope.getExtension("Core")
        compileScope.registerLazyClass(core, JavaObject::class.java)
        compileScope.registerLazyClass(core, WrapJavaObject::class.java)

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
    }

    /**
     * 执行php文件
     * @param bootstrapFile php入口文件
     * @param args 参数
     * @param out 输出流
     * @param outputBuffering 是否打开缓冲区
     * @param exceptionHandler 一次性异常处理器，该php文件执行后会恢复原来的异常处理器
     */
    fun run(bootstrapFile: String, args: Map<String, Any?> = emptyMap(), out: OutputStream? = null, outputBuffering: Boolean = true, exceptionHandler: ExceptionHandler? = null) {
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
            bootstrap.includeNoThrow(environment, locals)
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