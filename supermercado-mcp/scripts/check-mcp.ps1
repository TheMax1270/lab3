param([string]$Url = 'http://localhost:8088/mcp')
$ErrorActionPreference = 'Stop'
$headers = @{ Accept = 'application/json, text/event-stream' }
function Send-Mcp($payload) {
    $response = Invoke-WebRequest -UseBasicParsing -Uri $Url -Method Post -ContentType 'application/json' -Headers $headers -Body ($payload | ConvertTo-Json -Depth 10 -Compress) -TimeoutSec 20
    if ($response.Headers['Mcp-Session-Id']) { $headers['Mcp-Session-Id'] = $response.Headers['Mcp-Session-Id'] }
    if (!$response.Content) { return }
    $content = [string]$response.Content
    if ($content.TrimStart().StartsWith('{')) { $result = $content | ConvertFrom-Json }
    else {
        $line = ($content -split "`n" | Where-Object { $_ -match '^data:\s*\{' } | Select-Object -Last 1)
        if (!$line) { throw "Respuesta MCP inesperada: $content" }
        $result = ($line -replace '^data:\s*','') | ConvertFrom-Json
    }
    if ($result.error) { throw ($result.error | ConvertTo-Json -Compress) }
    return $result.result
}
$init = Send-Mcp @{jsonrpc='2.0';id=1;method='initialize';params=@{protocolVersion='2024-11-05';capabilities=@{};clientInfo=@{name='lab-check';version='1.0'}}}
if (!$init.protocolVersion) { throw 'No se negocio protocolo MCP' }
$headers['MCP-Protocol-Version'] = $init.protocolVersion
Send-Mcp @{jsonrpc='2.0';method='notifications/initialized'} | Out-Null
$list = Send-Mcp @{jsonrpc='2.0';id=2;method='tools/list';params=@{}}
$expected = @('listarProductos','buscarProducto','crearPedido','agregarProductoPedido','consultarPedido','confirmarPedido','cancelarPedido')
if (@($list.tools).Count -ne 7 -or @(Compare-Object $expected @($list.tools.name)).Count -ne 0) { throw 'Las herramientas publicadas no coinciden con las siete de supermercado' }
$read = Send-Mcp @{jsonrpc='2.0';id=3;method='tools/call';params=@{name='listarProductos';arguments=@{}}}
if ($read.isError) { throw 'Fallo listarProductos' }
Write-Host "MCP OK: initialize, 7 herramientas de supermercado y listarProductos. Protocolo $($init.protocolVersion)"
$list.tools.name | ForEach-Object { Write-Host " - $_" }
