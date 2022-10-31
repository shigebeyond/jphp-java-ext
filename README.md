# jphp-java-ext -- jphp扩展，用于增强对java对象的操作

## 1 增强执行php的API
主要的作用是：
1. 简化调用；
2. 开放参数来指定php文件名+变量，自动将java变量转换为php变量；
3. 支持多线程执行，每个线程独有一个jphp引擎；
4. 作为模板引擎的基础。

demo如下：
```
import net.jkcode.jphp.ext.JphpLauncher

// 执行 index.php 文件，并设置变量$name = "shi"
JphpLauncher.run("index.php" /* php 文件路径 */, mapOf("name" to "shi") /* 包含变量名与变量值的map */)
```
## 2 增强对java对象的操作
主要是实现`WrapJavaObject`，方便包装java对象，并在php中直接读写属性与调用方法

### 2.1 java调用端
```
@Test
fun testJphpLauncher(){
    val lan = JphpLauncher
    val data = mapOf(
            "name" to "shi",
            "maparray" to mapOf("age" to 11, "addr" to "nanning"), // 会转换php的array类型（即java的ArrayMemory）
            // WrapJavaObject
            "mapjo" to WrapJavaObject.of(lan.environment, mapOf("goods_id" to 1, "goods_name" to "火龙果", "quantity" to 13)),
            "pojo" to WrapJavaObject.of(lan.environment, Message("title", "jkcode代码库"))
    )
    lan.run("src/test/resources/index.php", data)
}
```

### 2.2 php渲染端
1. 使用 WrapJavaObject 包含变量
```
use php\lang\WrapJavaObject;
// 包装string类型java对象
$strjo = new WrapJavaObject($name);
// echo $strjo->length()."\n";
// echo $strjo->concat(" is hero")."\n";
echo $strjo->substring(3)."\n"; 
```

2. 使用 java塞进来的 WrapJavaObject 对象
2.1 包装hashmap
```
// 包装hashmap类型java对象
echo 'size: '. $mapjo->size()."\n";
$mapjo->put('price', '11.1');
echo 'size: '. $mapjo->size()."\n";
echo $mapjo->get('goods_name')."\n";
echo $mapjo->get('price')."\n";
```

2.2 包装pojo
```
// 包装简单java对象
echo $pojo->getKey()."\n"; // 调用方法
echo $pojo->getMessage()."\n";
$pojo->key = 'title2'; // 写属性，先尝试调用setter方法，然后写属性
echo $pojo->key."\n"; // 读属性, 先尝试调用getter方法，然后读属性
```

## 3 支持php文件的重载
包含以下支持
1. 检测文件变化
2. 卸载旧php文件的模块/类/方法等
3. 加载新php文件的模块/类/方法等

## 4 整合到jkguard库
抽象 IMethodMeta 体系, 以便兼容java方法与php方法, 从而将java/php方法调用都纳入 jkguard的守护体系中

详见[《jkguard整合jphp-守护php方法》](https://github.com/shigebeyond/jkguard/blob/master/doc/jphp.md)

## 5 整合到jkmvc框架
支持php写controller

详见[jkmvc整合jphp](https://github.com/shigebeyond/jkmvc/blob/master/doc/http/jphp.cn.md)

## 6 整合到jksoa框架
支持php调用rpc服务
详见[jksoa整合jphp](https://github.com/shigebeyond/jksoa/blob/master/doc/rpc/client/jphp.md)

## 7 性能最好的模板引擎

### 7.1 性能对比
针对 velocity / freemarker / jphp 3个模板引擎分别做了性能测试。

1. 测试思路
3个模板引擎对同样逻辑的模板，各自渲染1000次，对比各自耗时。
详见代码[TemplateTests.kt](https://github.com/shigebeyond/jkmvc/blob/master/jkmvc-http/src/test/kotlin/net/jkcode/jkmvc/tests/TemplateTests.kt)

2. 测试结果
3个模板引擎的测试结果如下
```
执行runJphp()耗时: 8s
执行runVelocity()耗时: 36s
执行runFreemarker()耗时: 14s
```
=> jphp是性能最好的模板引擎，因为jphp引擎会将php代码编译为等价的java字节码，因此他的执行效率是最高的

### 7.2 附上3个模板引擎对应的测试模板代码
1. jphp引擎：
test.php
```
<?php
for ($i=0; $i <= 9999; $i++)
{
    echo "Hello $name\n";
    foreach($friends as $f){
        if($i % 2 == 0)
            echo "-$f\n";
        else
            echo "+$f\n";
    }
}
```

2. velocity引擎：
test.vm
```
#foreach($i in [0..9999])
    Hello $name
    #foreach($f in $friends)
        #if($i % 2 == 0)
            -$f
        #else
            +$f
        #end
    #end
#end
```

3. freemarker引擎：
test.ftl
```
<#list 0..9999 as i>
    Hello ${name}
    <#list friends as f>
        <#if i % 2 == 0>
            -${f}
        <#else>
            +${f}
        </#if>
    </#list>
</#list>
```