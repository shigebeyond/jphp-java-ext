package net.jkcode.jphp.ext

import net.jkcode.jkutil.common.getAccessibleField
import php.runtime.env.CompileScope
import php.runtime.env.Environment
import php.runtime.env.ModuleManager
import php.runtime.reflection.ConstantEntity
import php.runtime.reflection.FunctionEntity
import php.runtime.reflection.ModuleEntity
import java.io.File

/**
 * 改写 ModuleManager.fetchModule()
 *   1 支持在卸载模块时, 也卸载模块的类/函数/常量
 *   2 TODO： 时间变了，但内容没变，增加md5(代码)的检查
 */
class JkModuleManager(env: Environment): ModuleManager(env) {

    override fun fetchModule(path: String, compiled: Boolean): ModuleEntity? {
        var moduleEntity = modules[path]
        if (moduleEntity != null &&
                (moduleEntity.context.lastModified == 0L || moduleEntity.context.lastModified == File(path).lastModified())) {
            //commonLogger.info("命中缓存的模块: " + moduleEntity.name)
            return moduleEntity
        }

        // bug: 只卸载文件，但没有卸载类/函数/常量
        //env.scope.removeUserModule(path)
        // 解决: 卸载文件/类/函数/常量, 要调用 CompileScope.unregisterModule() + Environment.unregisterModule()
        val oldMod = env.scope.unregisterModule(path)
        if(oldMod != null) {
            //commonLogger.info("检测模块有改动, 删除旧模块: " + oldMod.name)
            env.unregisterModule(oldMod)
        }

        //commonLogger.info("编译模块: " + path)
        moduleEntity = fetchTemporaryModule(path, compiled)
        if (moduleEntity == null)
            return null
        modules[path] = moduleEntity

        return moduleEntity
    }

}

// Environment.moduleManager字段
internal val moduleManagerField = Environment::class.java.getAccessibleField("moduleManager")!!
// Environment.functionMap字段
internal val functionMapField = Environment::class.java.getAccessibleField("functionMap")!!
// Environment.constantMap字段
internal val constantMapField = Environment::class.java.getAccessibleField("constantMap")!!

/**
 * 卸载模块
 *   1. 卸载模块(php文件)
 *   2. 卸载模块相关的类、方法、常量
 * @param name
 */
fun CompileScope.unregisterModule(name: String): ModuleEntity? {
    // 删除模块
    val module = removeUserModule(name) ?: return null

    // 删除模块相关的类、方法、常量
    for (entity in module.classes) {
        if (entity.isStatic) {
            val clazz = classMap.remove(entity.lowerName)
            clazz?.markUnregistered() // 标记php类注销
        }
    }
    for (entity in module.functions) {
        if (entity.isStatic) {
            functionMap.remove(entity.lowerName)
        }
    }
    for (entity in module.constants) {
        constantMap.remove(entity.lowerName)
    }

    return module
}

/**
 * 卸载模块相关的类、方法、常量
 *    因为 Environment#isLoadedClass() 会检查是否有重复类定义, 有则报错, 因此卸载模块时也要卸载相关类
 * @param name
 */
fun Environment.unregisterModule(module: ModuleEntity){
    // 删除模块相关的类、方法、常量
    for (entity in module.classes) {
        if (entity.isStatic) {
            classMap.remove(entity.lowerName)
        }
    }

    // functionMap是保护属性, 用反射来写
    val functionMap = functionMapField.get(this) as MutableMap<String, FunctionEntity>
    for (entity in module.functions) {
        if (entity.isStatic) {
            functionMap.remove(entity.lowerName)
        }
    }

    // constantMap是保护属性, 用反射来写
    val constantMap = constantMapField.get(this) as MutableMap<String, ConstantEntity>
    for (entity in module.constants) {
        constantMap.remove(entity.lowerName)
    }
}