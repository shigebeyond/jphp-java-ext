package net.jkcode.jphp.ext

import net.jkcode.jkutil.cache.ICache
import net.jkcode.jkutil.common.getMethodByName
import net.jkcode.jkutil.common.ucFirst
import php.runtime.Memory
import php.runtime.annotation.Reflection
import php.runtime.env.Environment
import php.runtime.ext.java.JavaClass
import php.runtime.ext.java.JavaMethod
import php.runtime.ext.java.JavaObject
import php.runtime.ext.java.JavaReflection
import php.runtime.lang.BaseWrapper
import php.runtime.memory.ObjectMemory
import php.runtime.memory.StringMemory
import php.runtime.memory.support.MemoryUtils
import php.runtime.reflection.ClassEntity
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit

/**
 * 包装cache
 *    php中的实例化: $cache = Cache::instance("jedis");
 *    php中的方法调用: $cache->get("key");
 */
@Reflection.Name("php\\lang\\Cache")
open class WrapCache(env: Environment, clazz: ClassEntity) : BaseWrapper<JavaObject>(env, clazz) {

    // 缓存配置名
    lateinit var name: String

    /**
     * 代理的缓存
     */
    val proxyCache: ICache by lazy{
        ICache.instance(name)
    }

    @Reflection.Signature
    fun __construct(name: String): Memory {
        if (name.isBlank())
            this.name = "jedis"
        else
            this.name = name
        return Memory.NULL
    }

    /**
     * 根据键获得值
     *
     * @param key 键
     * @return
     */
    @Reflection.Signature
    fun get(key: String): Memory {
        val v = proxyCache.get(key)
        if (v == null)
            return Memory.NULL
        return MemoryUtils.valueOf(v)
    }

    /**
     * 设置键值
     *   如果源数据是null, 则缓存空对象, 防止缓存穿透
     *
     * @param key 键
     * @param value 值
     * @param expireSencond 过期秒数
     */
    @Reflection.Signature
    fun put(key: String, value: Any?, expireSencond: Long) {
        proxyCache.put(key, value, expireSencond)
    }

    /**
     * 设置键值, 给了默认的过期秒数
     *   相当于 $this->put($key, $value, 6000);
     *
     * @param key 键
     * @param value 值
     */
    @Reflection.Signature
    fun set(key: String, value: Any?) {
        put(key, value, 6000)
    }

    /**
     * 删除指定的键的值
     * @param key 要删除的键
     */
    @Reflection.Signature
    fun remove(key: String) {
        proxyCache.remove(key)
    }

    /**
     * 删除指定正则的值
     * @param pattern 要删除的键的正则
     */
    @Reflection.Signature
    fun removeByPattern(pattern: String) {
        proxyCache.removeByPattern(pattern)
    }

    /**
     * 清空缓存
     */
    @Reflection.Signature
    fun clear() {
        proxyCache.clear()
    }

    companion object {

        @Reflection.Signature
        @JvmStatic
        fun instance(env: Environment, name: String = "jedis"): Memory {
            val cache = WrapCache(env, env.fetchClass("php\\lang\\WrapCache"))
            cache.name = name
            return ObjectMemory(cache)
        }
    }

}