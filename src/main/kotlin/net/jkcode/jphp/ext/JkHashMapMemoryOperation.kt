package net.jkcode.jphp.ext

import php.runtime.memory.support.operation.map.MapMemoryOperation

class JkHashMapMemoryOperation : MapMemoryOperation(Any::class.java, Any::class.java) {

    override fun getOperationClasses(): Array<Class<*>> {
        return arrayOf(HashMap::class.java)
    }

    override fun createHashMap(): Map<*, *> {
        return HashMap<Any?, Any?>()
    }
}
