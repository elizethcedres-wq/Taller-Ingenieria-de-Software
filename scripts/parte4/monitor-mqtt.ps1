$ErrorActionPreference = 'Stop'
Set-Location -LiteralPath (Resolve-Path (Join-Path $PSScriptRoot '../..')).Path
Write-Host 'Monitor MQTT de la parte 4. Dejar abierto durante la demostracion.'
Write-Host 'Ctrl+C cierra el monitor; los servicios siguen funcionando.'
& docker compose exec -T mosquitto mosquitto_sub -h localhost -t 'turnos/#' -q 1 -v
if ($LASTEXITCODE -ne 0) {
    throw 'No se pudo abrir el monitor MQTT. Revisar Docker Desktop y Mosquitto.'
}
