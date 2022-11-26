<?php
// 变量
echo "Hello $name\n";

// 日志
use php\lang\Log;
Log::info("hello {}", ["shi"]);

// 正则
use php\lang\Reg;
$reg = "\\d(\\w)";
$input = "1a 2a 3b";
var_dump(Reg::find($reg, $input));
var_dump(Reg::findAll($reg, $input));//每个匹配一行
var_dump(Reg::findAllInvert($reg, $input));//每组一行
echo Reg::replace($reg, 'hello', $input);
echo "\n";
var_dump(Reg::split(' ', $input));

// 正则
use php\util\Regex;
$reg = new Regex('[0-9]+', 0, "shi123");
echo $reg->replace("-ge");
echo "\n";
echo Regex::match('^[0-9]+$', '03894') == 1;
echo "\n";
var_dump(Regex::split('[0-9]+', 'foo93894bar840'));

// 数组操作
var_dump($maparray);
echo $maparray['age']."\n";
$maparray['sex'] = 'man';
var_dump($maparray);

// 文件操作
use php\io\File;
$file = new File('/ohome/shi/code/jphp/jphp/sandbox/src/JPHP-INF/launcher.conf');
echo "$file \n";
echo 'exist: '. $file->exists() . "\n";

// 缓存操作
use php\lang\Cache;
$cache = Cache::instance("jedis");
$cache->set("key", "xxxx");
echo $cache->get("key");

// java对象创建+调用
use php\lang\JavaClass;
$cls = new JavaClass("java.util.HashMap");
$obj = $cls->newInstance(); // 返回的是 JavaObject
var_dump($obj);
// echo 'size: '. $obj->size() . "\n"; // 报错: Call to undefined method php\lang\JavaObject::size()
$method = $cls->getDeclaredMethod('size');
echo 'size: '. $method->invoke($obj) . "\n";
echo 'size: '. $method->invokeArgs($obj, []) . "\n";

// 包装java，方便调用java方法
use php\lang\PJavaObject;
// 包装string类型java对象
$strjo = new PJavaObject($name);
// echo $strjo->length()."\n";
// echo $strjo->concat(" is hero")."\n";
echo $strjo->substring(3)."\n";

class A{}
class B extends A{
}

// 包装hashmap类型java对象
echo 'size: '. $mapjo->size()."\n";
$mapjo->put('price', '11.1');
echo 'size: '. $mapjo->size()."\n";
echo $mapjo->get('goods_name')."\n";
echo $mapjo->get('price')."\n";

// 包装简单java对象
echo $pojo->getKey()."\n"; // 调用方法
echo $pojo->getMessage()."\n";
$pojo->key = 'title2'; // 写属性，先尝试调用setter方法，然后写属性
echo $pojo->key."\n"; // 读属性, 先尝试调用getter方法，然后读属性

/*
echo __FILE__."\n";
echo dirname(__FILE__)."\n";
include 'src/test/resources/performance.php';
*/

return [
    'name' => 'shi'
];