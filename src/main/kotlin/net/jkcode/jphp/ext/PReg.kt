package net.jkcode.jphp.ext

import php.runtime.annotation.Reflection
import php.runtime.env.Environment
import php.runtime.lang.BaseObject
import java.util.regex.Matcher

@Reflection.Name("php\\lang\\Reg")
class PReg(env: Environment) : BaseObject(env) {

    companion object {

        /**
         * 找单个
         */
        @Reflection.Signature
        @JvmStatic
        fun find(pattern: String, subject: String): List<String>?{
            val r = pattern.toRegex().find(subject)
            return r?.groupValues
        }

        /**
         * 找多个
         */
        @Reflection.Signature
        @JvmStatic
        fun findAll(pattern: String, subject: String): List<List<String>> {
            val rs = pattern.toRegex().findAll(subject)
            return rs.mapTo(ArrayList()) {
                it.groupValues
            }
        }

        /**
         * 替换
         */
        @Reflection.Signature
        @JvmStatic
        fun replace(pattern: String, replacement: String, subject: String): String {
            return pattern.toRegex().replace(subject, replacement)
        }

        /**
         * 分割
         */
        @Reflection.Signature
        @JvmStatic
        @JvmOverloads
        fun split(pattern: String, subject: String, limit: Int = 0): List<String> {
            return pattern.toRegex().split(subject, limit)
        }

        /**
         * 转义
         */
        @Reflection.Signature
        @JvmStatic
        fun quote(str: String): String {
            return Matcher.quoteReplacement(str)
        }

    }
}