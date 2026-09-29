$root = Join-Path $PSScriptRoot '../src/main/resources'
function Write-Json($path, $value) {
    $dest = Join-Path $root $path
    New-Item -ItemType Directory -Force (Split-Path $dest) | Out-Null
    $value | ConvertTo-Json -Depth 30 | Set-Content -LiteralPath $dest -Encoding utf8
}
$names = @{
    me_io_hatch='ME Input/Output Hatch'
    mi_pattern_provider='MI Pattern Provider'
    advanced_mi_pattern_provider='Advanced MI Pattern Provider'
    extended_mi_pattern_provider='Extended MI Pattern Provider'
    combined_mi_pattern_provider='Advanced Extended MI Pattern Provider'
}
$lang = @{}
Add-Type -AssemblyName System.Drawing
$colors = @{mi_pattern_provider='#efba49';extended_mi_pattern_provider='#f4ca70';advanced_mi_pattern_provider='#ad83fa';combined_mi_pattern_provider='#bb93fa';me_io_hatch='#ad83fa'}
$idx = 0
foreach ($id in $names.Keys | Sort-Object) {
    $lang["block.modernenergistics.$id"] = $names[$id]
    Write-Json "assets/modernenergistics/blockstates/$id.json" @{variants=@{''=@{model="modernenergistics:block/$id"}}}
    Write-Json "assets/modernenergistics/models/block/$id.json" @{parent='minecraft:block/cube_all';textures=@{all="modernenergistics:block/$id"}}
    Write-Json "assets/modernenergistics/models/item/$id.json" @{parent="modernenergistics:block/$id"}
    if ($id -ne 'me_io_hatch') {
        $loot = @{type='minecraft:block';pools=@(@{rolls=1;entries=@(@{type='minecraft:item';name="modernenergistics:$id"});conditions=@(@{condition='minecraft:survives_explosion'})})}
        if ($id -eq 'advanced_mi_pattern_provider') { $loot['neoforge:conditions']=@(@{type='neoforge:mod_loaded';modid='advanced_ae'}) }
        if ($id -eq 'extended_mi_pattern_provider') { $loot['neoforge:conditions']=@(@{type='neoforge:mod_loaded';modid='extendedae'}) }
        if ($id -eq 'combined_mi_pattern_provider') { $loot['neoforge:conditions']=@(@{type='neoforge:mod_loaded';modid='advanced_ae'},@{type='neoforge:mod_loaded';modid='extendedae'}) }
        Write-Json "data/modernenergistics/loot_table/blocks/$id.json" $loot
    }
    $dest = Join-Path $root "assets/modernenergistics/textures/block/$id.png"
    New-Item -ItemType Directory -Force (Split-Path $dest) | Out-Null
    $bmp = [System.Drawing.Bitmap]::new(32,32)
    $g = [System.Drawing.Graphics]::FromImage($bmp)
    $g.Clear([System.Drawing.ColorTranslator]::FromHtml('#202b39'))
    $edge = [System.Drawing.SolidBrush]::new([System.Drawing.ColorTranslator]::FromHtml('#73889e'))
    $light = [System.Drawing.SolidBrush]::new([System.Drawing.ColorTranslator]::FromHtml('#a8b8c8'))
    $accent = [System.Drawing.SolidBrush]::new([System.Drawing.ColorTranslator]::FromHtml($colors[$id]))
    $g.FillRectangle($edge,1,1,30,3); $g.FillRectangle($edge,1,28,30,3)
    $g.FillRectangle($edge,1,4,3,24); $g.FillRectangle($edge,28,4,3,24)
    foreach ($x in @(5,25)) { foreach ($y in @(5,25)) { $g.FillRectangle($light,$x,$y,2,2) } }
    if ($id -eq 'me_io_hatch') {
        $g.FillRectangle($accent,7,9,18,4); $g.FillRectangle($accent,7,19,18,4)
        $g.FillRectangle($accent,21,7,4,8); $g.FillRectangle($accent,7,17,4,8)
    } else {
        foreach ($x in @(8,14,20)) { foreach ($y in @(8,14,20)) { $g.FillRectangle($accent,$x,$y,4,4) } }
    }
    $bmp.Save($dest,[System.Drawing.Imaging.ImageFormat]::Png)
    $g.Dispose(); $bmp.Dispose(); $edge.Dispose(); $light.Dispose(); $accent.Dispose()
    $idx++
}
Write-Json 'assets/modernenergistics/lang/en_us.json' $lang
$recipes = @{
    me_io_hatch=@{type='minecraft:crafting_shaped';pattern=@('IFI','PDP','IFI');key=@{I=@{item='modern_industrialization:advanced_item_input_hatch'};F=@{item='modern_industrialization:advanced_fluid_input_hatch'};P=@{item='ae2:interface'};D=@{item='minecraft:diamond'}};result=@{id='modernenergistics:me_io_hatch';count=1}}
    mi_pattern_provider=@{type='minecraft:crafting_shaped';pattern=@('IRI','RPR','IRI');key=@{I=@{item='minecraft:iron_ingot'};R=@{item='minecraft:redstone'};P=@{item='ae2:pattern_provider'}};result=@{id='modernenergistics:mi_pattern_provider';count=1}}
}
$variants = @(
    @('advanced_mi_pattern_provider','advanced_ae','advanced_ae:small_adv_pattern_provider'),
    @('extended_mi_pattern_provider','extendedae','extendedae:ex_pattern_provider'),
    @('combined_mi_pattern_provider','advanced_ae','modernenergistics:advanced_mi_pattern_provider')
)
foreach ($variant in $variants) {
    $conditions = @(@{type='neoforge:mod_loaded';modid=$variant[1]})
    $ingredients = @(@{item=$variant[2]},@{item='modernenergistics:mi_pattern_provider'})
    if ($variant[0] -eq 'combined_mi_pattern_provider') {
        $conditions += @{type='neoforge:mod_loaded';modid='extendedae'}
        $ingredients = @(@{item='modernenergistics:advanced_mi_pattern_provider'},@{item='modernenergistics:extended_mi_pattern_provider'})
    }
    $recipes[$variant[0]] = @{type='minecraft:crafting_shapeless';'neoforge:conditions'=$conditions;ingredients=$ingredients;result=@{id="modernenergistics:$($variant[0])";count=1}}
}
foreach ($id in $recipes.Keys) { Write-Json "data/modernenergistics/recipe/$id.json" $recipes[$id] }
Write-Json 'data/minecraft/tags/block/mineable/pickaxe.json' @{replace=$false;values=@($names.Keys | ForEach-Object {@{id="modernenergistics:$_";required=$false}})}

# Empty 16-cube GameTest structure, written as standard big-endian NBT.
$dest = Join-Path $root 'data/modernenergistics/structure/bridge_test.nbt'
New-Item -ItemType Directory -Force (Split-Path $dest) | Out-Null
$mem = [System.IO.MemoryStream]::new()
function Byte([byte]$n) { $mem.WriteByte($n) }
function Short([int]$n) { Byte (($n -shr 8) -band 255); Byte ($n -band 255) }
function Int([int]$n) { Byte (($n -shr 24) -band 255); Byte (($n -shr 16) -band 255); Byte (($n -shr 8) -band 255); Byte ($n -band 255) }
function Str([string]$s) { $bytes=[Text.Encoding]::UTF8.GetBytes($s); Short $bytes.Length; $mem.Write($bytes,0,$bytes.Length) }
Byte 10; Str ''
Byte 3; Str 'DataVersion'; Int 3955
Byte 9; Str 'size'; Byte 3; Int 3; Int 16; Int 16; Int 16
Byte 9; Str 'palette'; Byte 10; Int 1; Byte 8; Str 'Name'; Str 'minecraft:air'; Byte 0
Byte 9; Str 'blocks'; Byte 10; Int 0
Byte 9; Str 'entities'; Byte 10; Int 0
Byte 0
$file=[IO.File]::Create($dest)
$gzip=[IO.Compression.GZipStream]::new($file,[IO.Compression.CompressionMode]::Compress)
$bytes=$mem.ToArray(); $gzip.Write($bytes,0,$bytes.Length); $gzip.Dispose(); $file.Dispose(); $mem.Dispose()
& (Join-Path $PSScriptRoot 'provider-models.ps1')





