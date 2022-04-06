<?php
$start = time();
// 单纯加减，要转java Memory对象来加减，性能差 -- 157s
/* $r = 0;
for($i = 0; $i < 10000000000; $i++){
    if($i % 2 == 0)
        $r += $i;
    else
        $r -= $i;
} */
// 11s
for($i = 0; $i < 3000000; $i++){
    $map->put('name', 'shi');
    $s = $map->size();
    $map->get('age');
}
echo "php耗时: " . (time() - $start) . "s\n";