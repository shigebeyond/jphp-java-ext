<?php
namespace php\lang;

/**
 * Class WrapCache
 * @packages std, core
 */
class WrapCache
{

    /**
     * constructor.
     * @param string $name
     * @throws IOException
     */
    function __construct($name) {}

    /**
     * 根据键获得值
     *
     * @param $key 键
     * @return
     */
    function get($key){ }

    /**
     * 设置键值
     *   如果源数据是null, 则缓存空对象, 防止缓存穿透
     *
     * @param $key 键
     * @param $value 值
     * @param $expireSencond 过期秒数
     */
    function put($key, $value, $expireSencond)  { }

    /**
     * 设置键值, 给了默认的过期秒数
     *   相当于 $this->put($key, $value, 6000);
     *
     * @param $key 键
     * @param $value 值
     */
    function set($key, $value)  { }

    /**
     * 删除指定的键的值
     * @param $key 要删除的键
     */
    function remove(key) { }

    /**
     * 删除指定正则的值
     * @param pattern 要删除的键的正则
     */
    function removeByPattern($pattern) { }
    
    /**
     * 清空缓存
     */
    function clear() { }
    
}