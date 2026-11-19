param([ValidateSet('test','web','mcp','consulta')][string]$Action='web')
$ErrorActionPreference='Stop'
Set-Location $PSScriptRoot
# Solo se hereda una variable de entorno; nunca se lee una clave de archivos.
if (!$env:GOOGLE_API_KEY) { $env:GOOGLE_API_KEY=[Environment]::GetEnvironmentVariable('GOOGLE_API_KEY','User') }
if (!$env:GOOGLE_API_KEY) { $env:GOOGLE_API_KEY=[Environment]::GetEnvironmentVariable('GOOGLE_API_KEY','Machine') }
$env:GOOGLE_GENAI_USE_VERTEXAI='false'
if ($Action -eq 'test') { & .\mvnw.cmd -B -ntp verify }
else {
    $main=switch($Action) {
        web { 'com.example.agente.SupermercadoAgent' }
        mcp { 'com.example.agente.VerificarMcp' }
        consulta { 'com.example.agente.ConsultaProductos' }
    }
    & .\mvnw.cmd -B -ntp compile exec:java "-Dexec.mainClass=$main"
}
if ($LASTEXITCODE) { throw "La operacion termino con codigo $LASTEXITCODE" }
