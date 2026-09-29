# Restore the original nine-square provider face, with variant colors generated separately.
$resourceRoot = Join-Path $PSScriptRoot '../src/main/resources/assets/modernenergistics'
foreach ($id in @('mi_pattern_provider','advanced_mi_pattern_provider','extended_mi_pattern_provider','combined_mi_pattern_provider')) {
    foreach ($model in @($id, "${id}_oriented")) {
        @{parent='minecraft:block/cube_all';textures=@{all="modernenergistics:block/$id"}} |
            ConvertTo-Json -Depth 10 | Set-Content (Join-Path $resourceRoot "models/block/$model.json") -Encoding utf8
    }
    @{variants=@{''=@{model="modernenergistics:block/$id"}}} |
        ConvertTo-Json -Depth 10 | Set-Content (Join-Path $resourceRoot "blockstates/$id.json") -Encoding utf8
}
