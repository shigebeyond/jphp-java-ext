package net.jkcode.jkmvc.tests

import net.jkcode.jphp.ext.JphpLauncher
import net.jkcode.jkutil.common.getAccessibleField
import org.junit.Test
import php.runtime.env.Context
import php.runtime.env.Environment
import net.jkcode.jphp.ext.WrapJavaObject
import php.runtime.loader.compile.StandaloneCompiler
import php.runtime.memory.support.MemoryUtils
import java.io.File

data class Message(var key: String, var message: String)

class JphpTests {

    private val context = Context(File("unknown"))

    @Test
    fun testJphp(){
        val f = MemoryUtils::class.java.getAccessibleField("CONVERTERS")
        println(f!!.name)
    }

    @Test
    fun testJphpLauncher(){
        val lan = JphpLauncher.instance()
        val data = mapOf(
                "name" to "shi",
                "maparray" to mapOf("age" to 11, "addr" to "nanning"), // 会转换php的array类型（即java的ArrayMemory）
                // WrapJavaObject
                "mapjo" to WrapJavaObject.of(lan.environment, mapOf("goods_id" to 1, "goods_name" to "火龙果", "quantity" to 13)),
                "pojo" to WrapJavaObject.of(lan.environment, Message("title", "jkcode代码库"))
        )
        lan.run("src/test/resources/index.php", data)
    }

    @Test
    fun testCompilePhp(){
        // .php源文件目录
        val srcDir = "/home/shi/code/java/jphp-java-ext/src/test/resources"
        // .class编译文件的目录
        val destDir = srcDir + "/bin/"
        val env = Environment()
        val compiler = StandaloneCompiler(File(srcDir), env)
        // 编译
        compiler.compile(File(destDir), null)
        println("编译成功")
    }

    @Test
    fun testPerformanceJava(){
        for (p in 0 until 10) {
            val map = mutableMapOf<String, Any>("age" to 13)

            val start = System.currentTimeMillis()
            // 11s
            /*var r = 0L
        for(i in 0 until 10000000000){
            if(i % 2 == 0L)
                r += i
            else
                r -= i
        }*/
            // 0.045s
            for (i in 0 until 3000000) {
                map.put("name", "shi");
                val s = map.size
                map.get("age");
            }
            println("java耗时: " + (System.currentTimeMillis() - start) / 1000.0 + "s")
        }
    }

    @Test
    fun testPerformancePhp(){
        val lan = JphpLauncher.instance()
        val data = mapOf(
                "map" to WrapJavaObject.of(lan.environment, mutableMapOf("age" to 13))
        )
        for (p in 0 until 10) {
            lan.run("src/test/resources/performance.php", data)
        }
    }

}