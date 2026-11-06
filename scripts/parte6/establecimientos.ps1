$ErrorActionPreference = "Stop"
$base = "http://localhost:8080/api"
$archivoTemporal = [System.IO.Path]::GetTempFileName()

function Invocar-Api {
    param(
        [string]$Metodo,
        [string]$Ruta,
        $Datos = $null
    )

    Write-Host "`n$Metodo $Ruta"

    $argumentos = @(
        "--silent",
        "--show-error",
        "--fail-with-body",
        "--connect-timeout", "5",
        "--max-time", "30",
        "-X", $Metodo,
        "$base$Ruta"
    )

    if ($null -ne $Datos) {
        $json = $Datos | ConvertTo-Json -Depth 5 -Compress

        [System.IO.File]::WriteAllText(
            $archivoTemporal,
            $json,
            [System.Text.UTF8Encoding]::new($false)
        )

        $argumentos += @(
            "-H", "Content-Type: application/json",
            "--data-binary", "@$archivoTemporal"
        )
    }

    $respuesta = & curl.exe @argumentos

    if ($LASTEXITCODE -ne 0) {
        throw "La solicitud fallo. Respuesta: $respuesta"
    }

    if (-not [string]::IsNullOrWhiteSpace($respuesta)) {
        Write-Host $respuesta
        return ($respuesta | ConvertFrom-Json)
    }
}

try {
    # 1. Listar establecimientos.
    $null = Invocar-Api -Metodo GET -Ruta "/establecimientos"

    # 2. Crear un establecimiento de prueba.
    $datos = @{
        nombreComercial = "Local de prueba Parte 6"
        direccion = "Direccion de prueba 123"
        telefono = "099123456"
        correo = "local.parte6@example.com"
        horarioApertura = "08:00:00"
        horarioCierre = "18:00:00"
    }

    $creado = Invocar-Api -Metodo POST `
        -Ruta "/establecimientos" -Datos $datos

    if ($null -eq $creado.id) {
        throw "La API no devolvio el ID del establecimiento."
    }

    $idLocal = $creado.id
    Write-Host "ID creado: $idLocal"

    # 3. Consultar el establecimiento creado.
    $null = Invocar-Api -Metodo GET `
        -Ruta "/establecimientos/$idLocal"

    # 4. Modificar sus datos.
    $datos.nombreComercial = "Local Parte 6 actualizado"

    $null = Invocar-Api -Metodo PUT `
        -Ruta "/establecimientos/$idLocal" -Datos $datos

    # Comprobar que se guardo la modificacion.
    $actualizado = Invocar-Api -Metodo GET `
        -Ruta "/establecimientos/$idLocal"

    if ($actualizado.nombreComercial -ne $datos.nombreComercial) {
        throw "El nombre actualizado no coincide."
    }

    # 5. Eliminar exclusivamente el establecimiento creado.
    $null = Invocar-Api -Metodo DELETE `
        -Ruta "/establecimientos/$idLocal"

    Write-Host "`nOperaciones de establecimientos completadas."
}
finally {
    Remove-Item -LiteralPath $archivoTemporal -ErrorAction SilentlyContinue
}