$ErrorActionPreference = "Stop"

# Ubicarse en la raiz del proyecto.
Set-Location (Join-Path $PSScriptRoot "..\..")

$consulta = @"
SELECT 'ESTABLECIMIENTOS' AS seccion;
SELECT * FROM ESTABLECIMIENTO ORDER BY id DESC LIMIT 10;

SELECT 'PERSONAL' AS seccion;
SELECT * FROM PERSONAL ORDER BY id DESC LIMIT 10;

SELECT 'RESERVAS' AS seccion;
SELECT * FROM RESERVATURNO ORDER BY id DESC LIMIT 20;

SELECT 'CLIENTES' AS seccion;
SELECT * FROM CLIENTE ORDER BY id DESC LIMIT 10;

SELECT 'FACTURAS' AS seccion;
SELECT * FROM FACTURA ORDER BY id DESC LIMIT 10;

SELECT 'ITEMS DE FACTURA' AS seccion;
SELECT * FROM ITEMFACTURA ORDER BY id DESC LIMIT 10;
"@

docker compose exec -T mysql-db mysql `
    -uroot -proot `
    -D turnosdb `
    --table `
    -e $consulta

if ($LASTEXITCODE -ne 0) {
    throw "No se pudo consultar la base de datos."
}

Write-Host "`nConsulta de base de datos completada."