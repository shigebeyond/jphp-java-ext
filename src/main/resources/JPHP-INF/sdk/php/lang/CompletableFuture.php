<?php
namespace php\lang;

/**
 * Class CompletableFuture
 * @package php\lang
 */
class CompletableFuture {


    private function __construct() {}

    /**
     * @return bool
     */
    public function isCancelled() { }

    /**
     * @return bool
     */
    public function isDone() { }

    /**
     * @param bool $mayInterruptIfRunning
     * @return bool
     */
    public function cancel($mayInterruptIfRunning) { }

    /**
     * @param null|int $timeout - in milliseconds
     * @return mixed
     * @throws \Exception
     */
    public function get($timeout = null) { }

    // ---------------------- CompletableFuture 方法扩展 ----------------------
    /**
     * 设置后续回调
     * @param callable $callback 回调
     * @return CompletableFuture
     */
    public function thenApply(callable $callback) {}

    /**
     * 设置后续回调
     * @param callable $callback 回调
     * @return CompletableFuture
     */
    public function exceptionally(callable $callback) {}

    /**
     * 设置后续回调
     * @param callable $callback 回调
     * @return CompletableFuture
     */
    public function whenComplete(callable $callback) {}

    /**
     * 合并多个异步结果
     * @param array $futures
     * @return CompletableFuture
     */
    public function join(array $futures) {}

}
