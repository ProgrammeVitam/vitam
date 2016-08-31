<?php

$path = __DIR__ . '/../bower_components/material-design-icons';
$out = __DIR__ . '/../demo/js/data.js';
$codepoints = __DIR__ . '/../bower_components/material-design-icons/iconfont/codepoints';
$oldcodepoints = __DIR__ . '/../fonts/codepoints';

$start_js = 'window.data = ';
$end_js = ';';

//$categories = file_get_contents($out);
//$categories = substr(substr($categories, strlen($start_js)), 0, - strlen($end_js));
//$categories = json_decode($categories, true);
//$categories = $categories ?: [];

$categories = $icons = $old_icons = [];
$count = $count_cats = 0;

foreach (file($codepoints, FILE_IGNORE_NEW_LINES) as $code) {
    $code = explode(' ', $code, 2);
    $icons[$code[0]] = $code[1];
}
foreach (file($oldcodepoints, FILE_IGNORE_NEW_LINES) as $code) {
    $code = explode(' ', $code, 2);
    $old_icons[$code[0]] = $code[1];
}

foreach (array_diff(scandir($path), ['.', '..'])  as $category) {
    if (is_dir("$path/$category/svg/production")) {
        $count_cats ++;
        $categories[$category] = [];
        foreach (array_diff(scandir("$path/$category/svg/production"), ['.', '..']) as $file) {
            if (preg_match('/^ic_(.+?)_\d+px\.svg$/', $file, $match)) {
                $icon = $match[1];
                if (isset($icons[$icon])) {
                    if (!in_array($icons[$icon], $categories[$category])) {
                        $categories[$category][$icon] = $icons[$icon];
                        $count ++;
                    }
                }
            }
        }
    }
}

$new = 0;

foreach ($categories as &$category) {
    foreach ($category as $icon => &$code) {
        $c = $code;
        $code = [$c, !isset($old_icons[$icon])];
        if (!isset($old_icons[$icon])) {
            $new ++;
        }
    }
}


file_put_contents($out, $start_js . json_encode($categories) . $end_js);

echo "Found $count icons, ($new new) in $count_cats categories\n";