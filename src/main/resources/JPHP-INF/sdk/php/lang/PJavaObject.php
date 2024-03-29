<?php
namespace php\lang;

/**
 * Class PJavaObject
 * @packages std, core
 */
class PJavaObject
{

    /**
     * constructor.
     * @param string $obj
     * @throws IOException
     */
    function __construct($obj) {}

    function __call($method, $args) {}

    // ---------------- 改进final类JavaObject实现，属性读写先调动getter/setter ---------------
    /**
     * Get class of object
     * @return JavaClass
     */
    public function getClass() { }

    /**
     * Get name of class of object
     * @return string
     */
    public function getClassName() { }
}