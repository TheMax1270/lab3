param([ValidateSet('test','run','prepare','check')][string]$Action = 'test')
$ErrorActionPreference = 'Stop'
Set-Location $PSScriptRoot
if ($Action -eq 'check') { & "$PSScriptRoot\scripts\check-mcp.ps1"; return }
$bin = 'C:\Program Files\MySQL\MySQL Server 8.4\bin'
if (!(Test-Path "$bin\mysqld.exe")) { throw "Falta MySQL Server 8.4 en $bin" }
$local = Join-Path $PSScriptRoot '.local'
$data = Join-Path $local 'mysql-data'
$secret = Join-Path $local 'mysql-password.txt'
New-Item $local -ItemType Directory -Force | Out-Null
# Solo inicializamos un directorio nuevo; nunca reinicializamos datos existentes.
if (!(Test-Path $data)) {
    & "$bin\mysqld.exe" --no-defaults --initialize-insecure "--basedir=$($bin | Split-Path)" "--datadir=$data"
    if ($LASTEXITCODE) { throw 'Fallo inicializando MySQL; revisar .local/mysql-data/*.err' }
}
$env:MYSQL_PWD = if (Test-Path $secret) { (Get-Content $secret -Raw).Trim() } else { '' }
try {
    & "$bin\mysql.exe" --no-defaults --protocol=TCP -h 127.0.0.1 -P 3306 -u root -N -e 'SELECT 1' 2>$null | Out-Null
    $connected = $LASTEXITCODE -eq 0
} catch { $connected = $false }
if (!$connected) {
    $listener = [System.Net.Sockets.TcpListener]::new([System.Net.IPAddress]::Loopback,3306)
    try { $listener.Start() } catch { throw '3306 ocupado por otra instancia. No se modifica. Revise credenciales/configuracion.' } finally { $listener.Stop() }
    Start-Process "$bin\mysqld.exe" -WindowStyle Hidden -ArgumentList @('--no-defaults', ('--basedir="{0}"' -f (Split-Path $bin)), ('--datadir="{0}"' -f $data), '--bind-address=127.0.0.1','--port=3306','--mysqlx=OFF', '--log-error=lab-mysql.err') | Out-Null
    $ready = $false
    for ($i=0; $i -lt 60; $i++) {
        Start-Sleep -Milliseconds 500
        try {
            & "$bin\mysql.exe" --no-defaults --protocol=TCP -h 127.0.0.1 -P 3306 -u root -N -e 'SELECT 1' 2>$null | Out-Null
            if ($LASTEXITCODE -eq 0) { $ready = $true; break }
        } catch {}
    }
    if (!$ready) { throw 'MySQL no arranco: revisar .local/mysql-data/lab-mysql.err' }
}
# Comprobar identidad antes de modificar usuarios o esquema.
$actual = & "$bin\mysql.exe" --no-defaults --protocol=TCP -h 127.0.0.1 -P 3306 -u root -N --raw -e 'SELECT @@datadir'
if ($LASTEXITCODE -or $actual.TrimEnd('\','/').Replace('/','\') -ne $data.TrimEnd('\')) { throw '3306 no pertenece a la instancia local; no se modifica.' }
if (!(Test-Path $secret)) {
    $password = [Guid]::NewGuid().ToString('N') + [Guid]::NewGuid().ToString('N')
    # Guardar antes de ALTER para permitir recuperacion ante interrupciones.
    Set-Content $secret $password -Encoding ASCII
    & "$bin\mysql.exe" --no-defaults --protocol=TCP -h 127.0.0.1 -P 3306 -u root -e "ALTER USER 'root'@'localhost' IDENTIFIED BY '$password';"
    if ($LASTEXITCODE) { throw 'No se pudo asignar la clave local.' }
    $env:MYSQL_PWD = $password
}
& "$bin\mysql.exe" --no-defaults --protocol=TCP -h 127.0.0.1 -P 3306 -u root -e 'CREATE DATABASE IF NOT EXISTS labmcp CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;'
if ($LASTEXITCODE) { throw 'No se pudo crear labmcp.' }
$env:PWD_UDEA = (Get-Content $secret -Raw).Trim()
Remove-Item Env:MYSQL_PWD
if ($Action -eq 'prepare') { Write-Host 'MySQL local preparado en 127.0.0.1:3306'; return }
if ($Action -eq 'run') { & .\mvnw.cmd -B -ntp spring-boot:run } else { & .\mvnw.cmd -B -ntp clean verify }
if ($LASTEXITCODE) { throw "Maven termino con codigo $LASTEXITCODE" }
