# Ejecutar en PowerShell con: . .\scripts\parte4\demo-parte4.ps1
# Prepara datos propios y prueba el recorrido hasta dos items por 3000.
# Acelera solo la fecha de dos reservas de esta ejecucion.
# Mantiene los intervalos de 60 y 300 segundos.

# Paso 15: Crear el script de pruebas
$ErrorActionPreference = 'Stop'
Set-Location -LiteralPath (Resolve-Path (Join-Path $PSScriptRoot '../..')).Path

# Los comandos Docker tambien deben detener el script cuando fallan.
function DockerParte4 {
    $salidaDockerParte4 = & docker @args
    $codigoDockerParte4 = $LASTEXITCODE
    if ($codigoDockerParte4 -ne 0) {
        throw "Docker fallo (codigo $codigoDockerParte4): $($args -join ' ')"
    }
    return $salidaDockerParte4
}

$null = DockerParte4 info --format '{{.ServerVersion}}'
$serviciosActivosParte4 = @(DockerParte4 compose ps --services --status running)
foreach ($servicioParte4 in @('mysql-db','mosquitto','app-listener',
        'api-rest','procesador-solicitudes','facturador')) {
    if ($servicioParte4 -notin $serviciosActivosParte4) {
        throw "Falta iniciar el servicio $servicioParte4 antes de ejecutar la prueba"
    }
}
DockerParte4 compose stop app-generador

$base = 'http://localhost:8080/api'
$trabajo = Join-Path $env:TEMP ('iis-parte4-' + [guid]::NewGuid().ToString('N'))
New-Item -ItemType Directory -Path $trabajo | Out-Null
$email = 'demo.parte4.' + (Get-Date -Format 'yyyy-MM-dd.HH-mm-ss-fff') + '@example.com'
$fecha = (Get-Date).AddDays(3).ToString('yyyy-MM-dd')
$hora = $fecha + 'T14:00:00'
$hora2 = $fecha + 'T14:30:00'

function Pedir([string]$metodo, [string]$ruta, $datos, [int]$esperado) {
    $salida = Join-Path $trabajo 'respuesta.json'
    $argsCurl = @('-sS', '--connect-timeout', '5', '--max-time', '20',
        '-X', $metodo, '-o', $salida, '-w', '%{http_code}', ($base + $ruta))
    if ($null -ne $datos) {
        $entrada = Join-Path $trabajo 'entrada.json'
        [IO.File]::WriteAllText($entrada,
            ($datos | ConvertTo-Json -Depth 8 -Compress),
            (New-Object Text.UTF8Encoding($false)))
        $argsCurl += @('-H', 'Content-Type: application/json',
            '--data-binary', ('@' + $entrada))
    }
    $status = & curl.exe @argsCurl
    if ($LASTEXITCODE -ne 0) { throw 'Fallo de curl' }
    $texto = [IO.File]::ReadAllText($salida)
    if ($metodo -ne 'GET') { Write-Host "$metodo $ruta -> HTTP $status" }
    if ($metodo -ne 'GET') { Write-Host $texto }
    if ([int]$status -ne $esperado) { throw "Se esperaba HTTP $esperado" }
    if ($texto.Trim()) { return ConvertFrom-Json $texto }
}

function Esperar([long]$id, [string]$estado, [int]$segundos = 90) {
    $limite = (Get-Date).AddSeconds($segundos)
    $proximoAvisoEspera = (Get-Date).AddSeconds(10)
    Write-Host "Esperando reserva $id -> $estado (hasta $segundos segundos)..."
    do {
        $r = Pedir 'GET' "/reservas/$id" $null 200
        if ($r.estado -eq $estado) { Write-Host "Reserva $id -> $estado"; return $r }
        if ($r.estado -like 'RECHAZADO*' -and $r.estado -ne $estado) {
            throw "La reserva $id termino en $($r.estado)"
        }
        if ((Get-Date) -ge $proximoAvisoEspera) {
            Write-Host "Reserva $id sigue en $($r.estado); esperando $estado..."
            $proximoAvisoEspera = (Get-Date).AddSeconds(10)
        }
        Start-Sleep -Seconds 2
    } while ((Get-Date) -lt $limite)
    throw "La reserva $id no llego a $estado en $segundos segundos"
}

function Reservar([long]$estId, [long]$perId, [string]$cuando) {
    return Pedir 'POST' '/reservas' @{
        emailSolicitante=$email
        telefonoSolicitante='099123456'
        establecimientoId=$estId
        personalId=$perId
        fechaHoraTurno=$cuando
    } 202
}

# Paso 16: Crear datos de prueba
$datosEst = @{
    nombreComercial='Demo Parte 4 A'
    direccion='Direccion de prueba'
    telefono='099123456'
    correo=$email
    horarioApertura='08:00:00'
    horarioCierre='18:00:00'
}
$estA = Pedir 'POST' '/establecimientos' $datosEst 201
$datosEst.nombreComercial = 'Demo Parte 4 B'
$estB = Pedir 'POST' '/establecimientos' $datosEst 201
$personal = Pedir 'POST' '/personal' @{
    nombre='Profesional demo parte 4'
    especialidad='Atencion general'
    costoConsulta=1500
    duracionEstandar=30
    estado=$true
    establecimientoId=$estA.id
} 201

# Paso 17: Probar reserva y horario ocupado
$valida = Reservar $estA.id $personal.id $hora
$null = Esperar $valida.id 'AGENDADO'
$ocupada = Reservar $estA.id $personal.id $hora
$null = Esperar $ocupada.id 'RECHAZADO_TURNO_OCUPADO'

DockerParte4 compose stop procesador-solicitudes
$horaLote = $fecha + 'T16:00:00'
$loteA = Reservar $estA.id $personal.id $horaLote
$loteB = Reservar $estA.id $personal.id $horaLote
DockerParte4 compose start procesador-solicitudes
$null = Esperar $loteA.id 'AGENDADO'
$null = Esperar $loteB.id 'RECHAZADO_TURNO_OCUPADO'

# Paso 18: Probar solicitudes inválidas
$noExiste = Reservar 9223372036854775807 $personal.id $hora2
$null = Esperar $noExiste.id 'RECHAZADO_SOLICITUD_NO_VALIDA'
$ajeno = Reservar $estB.id $personal.id $hora2
$null = Esperar $ajeno.id 'RECHAZADO_SOLICITUD_NO_VALIDA'
$pasado = (Get-Date).AddDays(-1).ToString('yyyy-MM-dd') + 'T14:00:00'
$vieja = Reservar $estA.id $personal.id $pasado
$null = Esperar $vieja.id 'RECHAZADO_SOLICITUD_NO_VALIDA'

# Paso 19: Cancelar y reutilizar el horario
$null = Pedir 'DELETE' "/reservas/$($valida.id)" $null 202
$null = Esperar $valida.id 'CANCELADO'
$reutilizada = Reservar $estA.id $personal.id $hora
$null = Esperar $reutilizada.id 'AGENDADO'
$segunda = Reservar $estA.id $personal.id $hora2
$null = Esperar $segunda.id 'AGENDADO'

# Paso 20: Mostrar estados en MySQL
$sql = 'SELECT id,emailSolicitante,personal_solicitado_id,' +
    'establecimiento_solicitado_id,personal_id,fechaHoraTurno,estado ' +
    "FROM RESERVATURNO WHERE emailSolicitante='$email' ORDER BY id;"
DockerParte4 compose exec -T mysql-db mysql -uroot -proot -D turnosdb -e $sql

# Paso 21: Acelerar solo las dos reservas propias
DockerParte4 compose stop procesador-solicitudes facturador
$finPrueba = (Get-Date).Date.AddDays(-1).AddHours(10)
$t1 = $finPrueba.ToString('yyyy-MM-ddTHH:mm:ss')
$t2 = $finPrueba.AddMinutes(30).ToString('yyyy-MM-ddTHH:mm:ss')
$sql = "UPDATE RESERVATURNO SET fechaHoraTurno='$t1' " +
    "WHERE id=$($reutilizada.id) AND emailSolicitante='$email' " +
    "AND estado='AGENDADO'; " +
    "UPDATE RESERVATURNO SET fechaHoraTurno='$t2' " +
    "WHERE id=$($segunda.id) AND emailSolicitante='$email' " +
    "AND estado='AGENDADO';"
DockerParte4 compose exec -T mysql-db mysql -uroot -proot -D turnosdb -e $sql

# Paso 22: Observar ATENDIDO
DockerParte4 compose start procesador-solicitudes
$null = Esperar $reutilizada.id 'ATENDIDO'
$null = Esperar $segunda.id 'ATENDIDO'
DockerParte4 compose logs --tail 80 procesador-solicitudes

# Paso 23: Observar FACTURADO
DockerParte4 compose start facturador
$null = Esperar $reutilizada.id 'FACTURADO' 330
$null = Esperar $segunda.id 'FACTURADO' 330
$sql = 'SELECT c.id,c.email,f.id AS factura_id,f.anio,f.mes,f.total,' +
    'i.id AS item_id,i.reserva_id,i.importe FROM CLIENTE c ' +
    'JOIN FACTURA f ON f.cliente_id=c.id ' +
    'JOIN ITEMFACTURA i ON i.factura_id=f.id ' +
    "WHERE c.email='$email' ORDER BY f.id,i.id;"
DockerParte4 compose exec -T mysql-db mysql -uroot -proot -D turnosdb -e $sql

# Paso 24: Comprobar que no repite la factura
DockerParte4 compose restart facturador
Start-Sleep -Seconds 5
DockerParte4 compose exec -T mysql-db mysql -uroot -proot -D turnosdb -e $sql

DockerParte4 compose stop facturador
$reintento = "UPDATE RESERVATURNO SET estado='ATENDIDO' " +
    "WHERE id=$($reutilizada.id) AND emailSolicitante='$email' " +
    "AND estado='FACTURADO';"
DockerParte4 compose exec -T mysql-db mysql -uroot -proot -D turnosdb -e $reintento
DockerParte4 compose start facturador
$null = Esperar $reutilizada.id 'FACTURADO' 330
DockerParte4 compose exec -T mysql-db mysql -uroot -proot -D turnosdb -e $sql

# Paso 25: Comprobar importes
$q = 'SELECT COUNT(*),SUM(i.importe) FROM ITEMFACTURA i ' +
    'JOIN FACTURA f ON i.factura_id=f.id ' +
    'JOIN CLIENTE c ON f.cliente_id=c.id ' +
    "WHERE c.email='$email';"
$resumen = & DockerParte4 compose exec -T mysql-db mysql -uroot -proot -N -B -D turnosdb -e $q
if ($LASTEXITCODE -ne 0) { throw 'Fallo la consulta de verificacion' }
$columnas = $resumen.Trim() -split "\s+"
if ([int]$columnas[0] -ne 2) { throw 'Se esperaban dos items' }
$suma = [decimal]::Parse($columnas[1],
    [Globalization.CultureInfo]::InvariantCulture)
if ($suma -ne 3000) { throw 'El importe esperado es 3000' }

# Verificar tambien que se reutilizaron un cliente y una factura.
$qAgrupacion = 'SELECT COUNT(DISTINCT c.id),COUNT(DISTINCT f.id),COUNT(i.id) ' +
    'FROM CLIENTE c JOIN FACTURA f ON f.cliente_id=c.id ' +
    "JOIN ITEMFACTURA i ON i.factura_id=f.id WHERE c.email='$email';"
$agrupacion = DockerParte4 compose exec -T mysql-db mysql -uroot -proot -N -B -D turnosdb -e $qAgrupacion
$cuentas = ($agrupacion -join "`n").Trim() -split '\s+'
if ($cuentas.Count -ne 3 -or [int]$cuentas[0] -ne 1 -or
        [int]$cuentas[1] -ne 1 -or [int]$cuentas[2] -ne 2) {
    throw 'Se esperaban un cliente, una factura y dos items'
}
$qFactura = 'SELECT f.total,f.anio,f.mes FROM FACTURA f ' +
    "JOIN CLIENTE c ON c.id=f.cliente_id WHERE c.email='$email';"
$importeFactura = DockerParte4 compose exec -T mysql-db mysql -uroot -proot -N -B -D turnosdb -e $qFactura
$datosFactura = ($importeFactura -join "`n").Trim() -split '\s+'
$totalFactura = [decimal]::Parse($datosFactura[0],
    [Globalization.CultureInfo]::InvariantCulture)
if ($totalFactura -ne 3000 -or [int]$datosFactura[1] -ne $finPrueba.Year -or
        [int]$datosFactura[2] -ne $finPrueba.Month) {
    throw 'La factura debe sumar 3000 y corresponder al periodo de los turnos'
}
$qReservas = 'SELECT COUNT(*) FROM ITEMFACTURA i ' +
    'JOIN FACTURA f ON f.id=i.factura_id JOIN CLIENTE c ON c.id=f.cliente_id ' +
    "WHERE c.email='$email' AND i.reserva_id IN ($($reutilizada.id),$($segunda.id));"
$itemsPropios = DockerParte4 compose exec -T mysql-db mysql -uroot -proot -N -B -D turnosdb -e $qReservas
if ([int]($itemsPropios -join '').Trim() -ne 2) {
    throw 'Los items deben corresponder a las dos reservas atendidas de esta prueba'
}
Write-Host 'PARTE4_OK: un cliente, una factura, dos items de 1500, total 3000.'
Write-Host "Email de esta prueba: $email"
Write-Host "Reservas facturadas: $($reutilizada.id), $($segunda.id)"
