package net.jkcode.jphp.ext

import org.slf4j.LoggerFactory
import php.runtime.Memory
import php.runtime.annotation.Reflection
import php.runtime.common.HintType
import php.runtime.env.Environment
import php.runtime.lang.BaseObject
import php.runtime.memory.ArrayMemory

@Reflection.Name("php\\lang\\Log")
class PLog(env: Environment) : BaseObject(env) {

    companion object {
        /**
         * 代理的日志对象
         */
        val logger = LoggerFactory.getLogger(PLog::class.java)

        @Reflection.Signature
        @JvmStatic
        @JvmOverloads
        fun trace(format: String, params: ArrayMemory? = null){
            logger.trace(format, *params.toPureArray())
        }

        @Reflection.Signature
        @JvmStatic
        @JvmOverloads
        fun debug(format: String, params: ArrayMemory? = null){
            logger.debug(format, *params.toPureArray())
        }

        @Reflection.Signature
        @JvmStatic
        @JvmOverloads
        fun warn(format: String, params: ArrayMemory? = null){
            logger.warn(format, *params.toPureArray())
        }

        @Reflection.Signature
        @JvmStatic
        @JvmOverloads
        fun info(format: String, params: ArrayMemory? = null){
            logger.info(format, *params.toPureArray())
        }

        @Reflection.Signature
        @JvmStatic
        @JvmOverloads
        fun error(format: String, params: ArrayMemory? = null){
            logger.error(format, *params.toPureArray())
        }

    }
}