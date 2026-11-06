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
    # Crear un local propio para esta demostracion.
    $local = @{
        nombreComercial = "Local prueba personal Parte 6"
        direccion = "Direccion de prueba 456"
        telefono = "099123456"
        correo = "personal.parte6@example.com"
        horarioApertura = "08:00:00"
        horarioCierre = "18:00:00"
    }

    $establecimiento = Invocar-Api -Metodo POST `
        -Ruta "/establecimientos" -Datos $local

    $idLocal = $establecimiento.id

    if ($null -eq $idLocal) {
        throw "No se obtuvo el ID del local."
    }

    # 1. Listar todo el personal.
    $null = Invocar-Api -Metodo GET -Ruta "/personal"

    # 2. Crear personal por la ruta general.
    $datos = @{
        nombre = "Profesional prueba Parte 6"
        especialidad = "Atencion general"
        costoConsulta = 1500
        duracionEstandar = 30
        estado = $true
        establecimientoId = $idLocal
    }

    $persona = Invocar-Api -Metodo POST `
        -Ruta "/personal" -Datos $datos

    $idPersonal = $persona.id

    if ($null -eq $idPersonal) {
        throw "No se obtuvo el ID del personal."
    }

    # 3. Consultar por ID.
    $null = Invocar-Api -Metodo GET `
        -Ruta "/personal/$idPersonal"

    # 4. Actualizar el personal.
    $datos.nombre = "Profesional Parte 6 actualizado"
    $datos.costoConsulta = 1800

    $null = Invocar-Api -Metodo PUT `
        -Ruta "/personal/$idPersonal" -Datos $datos

    $actualizado = Invocar-Api -Metodo GET `
        -Ruta "/personal/$idPersonal"

    if ($actualizado.nombre -ne $datos.nombre `
            -or $actualizado.costoConsulta -ne 1800) {
        throw "Los datos actualizados no coinciden."
    }

    # 5. Crear personal desde la ruta del establecimiento.
    $datosSegundo = @{
        nombre = "Segundo profesional Parte 6"
        especialidad = "Atencion general"
        costoConsulta = 1200
        duracionEstandar = 30
        estado = $true
    }

    $segundaPersona = Invocar-Api -Metodo POST `
        -Ruta "/establecimientos/$idLocal/personal" `
        -Datos $datosSegundo

    $idSegundo = $segundaPersona.id

    if ($null -eq $idSegundo) {
        throw "No se obtuvo el ID del segundo profesional."
    }

    # 6. Consultar el personal del establecimiento.
    $null = Invocar-Api -Metodo GET `
        -Ruta "/establecimientos/$idLocal/personal"

    # 7. Eliminar los dos profesionales de prueba.
    $null = Invocar-Api -Metodo DELETE `
        -Ruta "/personal/$idPersonal"

    $null = Invocar-Api -Metodo DELETE `
        -Ruta "/personal/$idSegundo"

    # Eliminar el local de prueba, que ya no tiene personal.
    $null = Invocar-Api -Metodo DELETE `
        -Ruta "/establecimientos/$idLocal"

    Write-Host "`nOperaciones de personal completadas."
}
finally {
    Remove-Item -LiteralPath $archivoTemporal -ErrorAction SilentlyContinue
}