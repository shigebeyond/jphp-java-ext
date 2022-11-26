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
         *   每个匹配一行，一行有多个分组
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
         * 找多个
         *   每组一行，一行只有一个分组，但包含该分组的所有匹配
         */
        @Reflection.Signature
        @JvmStatic
        fun findAllInvert(pattern: String, subject: String): List<List<String>> {
            val rs = pattern.toRegex().findAll(subject)
            val ngroup = rs.first().groups.size
            val gs = ArrayList<ArrayList<String>>(ngroup)
            for(i in 0 until ngroup)
                gs.add(ArrayList())
            for(r in rs){
                for(i in 0 until ngroup)
                    gs[i].add(r.groups[i]!!.value)
            }
            return gs
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