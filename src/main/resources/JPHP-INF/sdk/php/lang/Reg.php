<?php
namespace php\lang;

/**
 * Class Reg
 * @packages std, core
 */
class Reg
{

    /**
     * 找单个
     */
    static function find(string $pattern, string $subject): array {
    }

    /**
     * 找多个
     *   每个匹配一行
     */
    static function findAll(string $pattern, string $subject): array {
    }

    /**
     * 找多个
     *   每组一行
     */
    static function findAllInvert($pattern, $subject): array{
    }

    /**
     * 替换
     */
    static function replace(string $pattern, string $replacement, string $subject): String {
    }

    /**
     * 分割
     */
    static function split(string $pattern, string $subject, int $limit = 0): array {
    }

    /**
     * 转义
     */
    static function quote(str: String): String {
    }
}