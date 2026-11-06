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
 # Preparar un establecimiento propio.
    $local = @{
        nombreComercial = "Local prueba reservas Parte 6"
        direccion = "Direccion de prueba 789"
        telefono = "099123456"
        correo = "reservas.parte6@example.com"
        horarioApertura = "08:00:00"
        horarioCierre = "18:00:00"
    }

    $establecimiento = Invocar-Api -Metodo POST `
        -Ruta "/establecimientos" -Datos $local

    $idLocal = $establecimiento.id

    if ($null -eq $idLocal) {
        throw "No se obtuvo el ID del local."
    }

    # Preparar un empleado propio.
    $datosPersonal = @{
        nombre = "Profesional reservas Parte 6"
        especialidad = "Atencion general"
        costoConsulta = 1500
        duracionEstandar = 30
        estado = $true
        establecimientoId = $idLocal
    }

    $persona = Invocar-Api -Metodo POST `
        -Ruta "/personal" -Datos $datosPersonal

    $idPersonal = $persona.id

    if ($null -eq $idPersonal) {
        throw "No se obtuvo el ID del personal."
    }

    $fecha = (Get-Date).AddDays(5).ToString("yyyy-MM-dd")
    $fechaHora = "${fecha}T11:00:00"

    # 1. Listar reservas.
    $null = Invocar-Api -Metodo GET -Ruta "/reservas"

    # 2. Consultar disponibilidad antes de reservar.
    $disponibilidad = Invocar-Api -Metodo GET `
        -Ruta "/personal/$idPersonal/disponibilidad?fechaHora=$fechaHora"

    if ($disponibilidad.disponible -ne $true) {
        throw "El horario inicial no esta disponible."
    }

    # 3. Solicitar una reserva.
    $datosReserva = @{
        emailSolicitante = "reserva.parte6@example.com"
        telefonoSolicitante = "099123456"
        establecimientoId = $idLocal
        personalId = $idPersonal
        fechaHoraTurno = $fechaHora
    }

    $reserva = Invocar-Api -Metodo POST `
        -Ruta "/reservas" -Datos $datosReserva

    $idReserva = $reserva.id

    if ($null -eq $idReserva) {
        throw "No se obtuvo el ID de la reserva."
    }

    # 4. Consultar por ID hasta que sea evaluada.
    $limite = (Get-Date).AddSeconds(120)

    do {
        $actual = Invocar-Api -Metodo GET `
            -Ruta "/reservas/$idReserva"

        if ($actual.estado -ne "SOLICITADO") {
            break
        }

        if ((Get-Date) -ge $limite) {
            throw "La reserva no fue evaluada dentro de 120 segundos."
        }

        Start-Sleep -Seconds 3
    } while ($true)

    if ($actual.estado -ne "AGENDADO") {
        throw "La reserva termino en estado: $($actual.estado)"
    }

    # 5. Consultar la agenda del empleado para ese dia.
    $agenda = @(Invocar-Api -Metodo GET `
        -Ruta "/personal/$idPersonal/reservas?fecha=$fecha")

    if (-not ($agenda | Where-Object { $_.id -eq $idReserva })) {
        throw "La reserva no aparece en la agenda."
    }

    $ocupado = Invocar-Api -Metodo GET `
        -Ruta "/personal/$idPersonal/disponibilidad?fechaHora=$fechaHora"

    if ($ocupado.disponible -ne $false) {
        throw "El horario agendado deberia aparecer ocupado."
    }

    # 6. Solicitar la cancelacion.
    $null = Invocar-Api -Metodo DELETE `
        -Ruta "/reservas/$idReserva"

    $limite = (Get-Date).AddSeconds(30)

    do {
        $actual = Invocar-Api -Metodo GET `
            -Ruta "/reservas/$idReserva"

        if ($actual.estado -eq "CANCELADO") {
            break
        }

        if ((Get-Date) -ge $limite) {
            throw "La cancelacion no se completo dentro de 30 segundos."
        }

        Start-Sleep -Seconds 2
    } while ($true)

    # Comprobar que se libero el horario.
    $libre = Invocar-Api -Metodo GET `
        -Ruta "/personal/$idPersonal/disponibilidad?fechaHora=$fechaHora"

    if ($libre.disponible -ne $true) {
        throw "El horario cancelado no quedo disponible."
    }

    Write-Host "`nOperaciones de reservas completadas."
    Write-Host "Local: $idLocal | Personal: $idPersonal | Reserva: $idReserva"
}
    
finally {
    Remove-Item -LiteralPath $archivoTemporal -ErrorAction SilentlyContinue
}