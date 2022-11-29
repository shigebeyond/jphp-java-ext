package net.jkcode.jphp.ext

import net.jkcode.jkutil.common.ZipUtil
import php.runtime.Memory
import php.runtime.annotation.Reflection
import php.runtime.env.Environment
import php.runtime.lang.BaseObject
import php.runtime.memory.ArrayMemory

@Reflection.Name("php\\lang\\Zip")
class PZip(env: Environment) : BaseObject(env) {

    companion object {

        @Reflection.Signature
        @JvmStatic
        fun zip(src: Memory, destZip: String){
            val srcFiles: List<*>
            if(src is ArrayMemory)
                srcFiles = src.toPureList()
            else
                srcFiles = listOf(src.toString())
            ZipUtil.zip(srcFiles, destZip)
        }

        @Reflection.Signature
        @JvmStatic
        fun unzip(srcFile: String, destDir: String){
            ZipUtil.unzip(srcFile, destDir)
        }



    }
}