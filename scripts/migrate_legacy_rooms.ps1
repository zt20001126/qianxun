param(
    [switch]$Apply,
    [string]$SourceHost = '127.0.0.1',
    [int]$SourcePort = 3306,
    [string]$SourceUser = 'root',
    [string]$SourceDatabase = 'homestay_db',
    [string]$TargetHost = '127.0.0.1',
    [int]$TargetPort = 3307,
    [string]$TargetUser = 'qianxun',
    [string]$TargetDatabase = 'qianxun'
)

$ErrorActionPreference = 'Stop'
$mysqlPath = (Get-Command mysql.exe -ErrorAction Stop).Source
$allowedRoomNumbers = @('101', '102', '103', '201', '202', '203')

function Convert-SecureStringToPlainText([Security.SecureString]$SecureValue) {
    $pointer = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($SecureValue)
    try {
        return [Runtime.InteropServices.Marshal]::PtrToStringBSTR($pointer)
    } finally {
        [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($pointer)
    }
}

function Invoke-MySqlQuery(
    [string]$HostName,
    [int]$Port,
    [string]$UserName,
    [string]$Database,
    [string]$Password,
    [string]$Query
) {
    $previousPassword = $env:MYSQL_PWD
    $env:MYSQL_PWD = $Password
    try {
        $result = & $mysqlPath --host=$HostName --port=$Port --user=$UserName --database=$Database `
            --batch --raw --skip-column-names --default-character-set=utf8mb4 --execute=$Query 2>&1
        if ($LASTEXITCODE -ne 0) {
            throw "MySQL 查询失败：$($result -join [Environment]::NewLine)"
        }
        return $result
    } finally {
        if ($null -eq $previousPassword) {
            Remove-Item Env:MYSQL_PWD -ErrorAction SilentlyContinue
        } else {
            $env:MYSQL_PWD = $previousPassword
        }
    }
}

function Convert-JsonStringArray([string]$JsonValue, [string]$FieldName, [string]$RoomNumber) {
    if ([string]::IsNullOrWhiteSpace($JsonValue)) {
        return ,@()
    }
    try {
        $parsed = ConvertFrom-Json -InputObject $JsonValue
        $values = @($parsed | ForEach-Object { [string]$_ })
        return ,$values
    } catch {
        throw "房间 $RoomNumber 的 $FieldName 不是有效 JSON 数组，停止导入。"
    }
}

function Convert-TextToMySqlHex([string]$Value) {
    if ($null -eq $Value) {
        return 'NULL'
    }
    $bytes = [Text.Encoding]::UTF8.GetBytes($Value)
    $hexValue = [Convert]::ToHexString($bytes)
    return "CONVERT(0x$hexValue USING utf8mb4)"
}

function Convert-JsonArrayToMySql([string[]]$Values) {
    $json = ConvertTo-Json -InputObject @($Values) -Compress -Depth 10
    $bytes = [Text.Encoding]::UTF8.GetBytes($json)
    $hexValue = [Convert]::ToHexString($bytes)
    return "CAST(CONVERT(0x$hexValue USING utf8mb4) AS JSON)"
}

function Format-Decimal([object]$Value, [int]$Scale) {
    $decimalValue = [decimal]::Parse([string]$Value, [Globalization.CultureInfo]::InvariantCulture)
    return $decimalValue.ToString("F$Scale", [Globalization.CultureInfo]::InvariantCulture)
}

$sourceSecurePassword = Read-Host "输入只读源库 $SourceDatabase 的 MySQL 密码" -AsSecureString
$sourcePassword = Convert-SecureStringToPlainText $sourceSecurePassword

$sourceQuery = @"
SELECT JSON_OBJECT(
    'number', r.room_number,
    'name', rt.name,
    'floor', COALESCE(r.floor, CAST(SUBSTRING(r.room_number, 1, 1) AS UNSIGNED)),
    'price', CAST(rt.base_price AS CHAR),
    'capacity', COALESCE(rt.max_guests, 2),
    'bed_type', rt.bed_type,
    'bed_count', rt.bed_count,
    'area', IF(rt.area IS NULL, NULL, CAST(rt.area AS CHAR)),
    'facilities', rt.facilities,
    'images', rt.images,
    'description', rt.description,
    'enabled', CASE WHEN COALESCE(rt.status, 1) = 1 THEN TRUE ELSE FALSE END
)
FROM room r
JOIN room_type rt ON rt.id = r.room_type_id
WHERE COALESCE(r.deleted, 0) = 0 AND COALESCE(rt.deleted, 0) = 0
ORDER BY r.room_number
"@

try {
    $sourceRows = Invoke-MySqlQuery $SourceHost $SourcePort $SourceUser $SourceDatabase $sourcePassword $sourceQuery
} finally {
    $sourcePassword = $null
    $sourceSecurePassword.Dispose()
}

$rooms = @()
foreach ($line in $sourceRows) {
    if ([string]::IsNullOrWhiteSpace($line)) {
        continue
    }
    $rawRoom = ConvertFrom-Json -InputObject $line
    $number = [string]$rawRoom.number
    if ($number -notin $allowedRoomNumbers) {
        throw "源库出现非本店固定房号 $number，停止导入。"
    }
    $floor = [int]$rawRoom.floor
    $expectedFloor = if ($number.StartsWith('1')) { 1 } else { 2 }
    if ($floor -ne $expectedFloor) {
        throw "房间 $number 的楼层与房号不一致，停止导入。"
    }

    $bedType = if ($null -eq $rawRoom.bed_type) { '' } else { ([string]$rawRoom.bed_type).Trim() }
    $bedCount = if ($null -eq $rawRoom.bed_count) { 1 } else { [int]$rawRoom.bed_count }
    $bedInfo = $bedType
    if ($bedInfo -and $bedCount -gt 1 -and $bedInfo -notmatch "(?i)x\s*$bedCount(?:\D|$)") {
        $bedInfo = "$bedInfo × $bedCount 张"
    }

    $rooms += [pscustomobject]@{
        number = $number
        name = [string]$rawRoom.name
        floor = $floor
        price = Format-Decimal $rawRoom.price 2
        capacity = [int]$rawRoom.capacity
        bed_info = if ([string]::IsNullOrWhiteSpace($bedInfo)) { $null } else { $bedInfo }
        area = if ($null -eq $rawRoom.area) { $null } else { Format-Decimal $rawRoom.area 2 }
        facilities = Convert-JsonStringArray ([string]$rawRoom.facilities) 'facilities' $number
        images = Convert-JsonStringArray ([string]$rawRoom.images) 'images' $number
        description = if ([string]::IsNullOrWhiteSpace([string]$rawRoom.description)) { $null } else { [string]$rawRoom.description }
        enabled = [bool]$rawRoom.enabled
    }
}

if ($rooms.Count -ne $allowedRoomNumbers.Count) {
    throw "源库有效客房数量为 $($rooms.Count)，预期为 $($allowedRoomNumbers.Count)，停止导入。"
}
foreach ($number in $allowedRoomNumbers) {
    if (-not ($rooms | Where-Object number -eq $number)) {
        throw "源库缺少固定房间 $number，停止导入。"
    }
}

Write-Host "只读读取到 $($rooms.Count) 间客房："
$rooms | Sort-Object number | ForEach-Object {
    Write-Host ("  {0}  {1}  ¥{2}/晚  {3} 人  {4} 张图片" -f $_.number, $_.name, $_.price, $_.capacity, $_.images.Count)
}

if (-not $Apply) {
    Write-Host '当前为预览模式；未连接或写入目标库。确认映射后使用 -Apply 导入。'
    exit 0
}

$targetSecurePassword = Read-Host "输入目标开发库 $TargetDatabase 的 MySQL 密码" -AsSecureString
$targetPassword = Convert-SecureStringToPlainText $targetSecurePassword
try {
    $existingRows = Invoke-MySqlQuery $TargetHost $TargetPort $TargetUser $TargetDatabase $targetPassword 'SELECT number FROM room'
    $existingNumbers = @($existingRows | Where-Object { -not [string]::IsNullOrWhiteSpace($_) })
    $roomsToInsert = @($rooms | Where-Object { $_.number -notin $existingNumbers })
    if ($roomsToInsert.Count -eq 0) {
        Write-Host '目标库已包含全部房号，没有新增写入。'
        exit 0
    }

    $valueRows = foreach ($room in $roomsToInsert) {
        $numberSql = Convert-TextToMySqlHex $room.number
        $nameSql = Convert-TextToMySqlHex $room.name
        $bedInfoSql = Convert-TextToMySqlHex $room.bed_info
        $descriptionSql = Convert-TextToMySqlHex $room.description
        $facilitiesSql = Convert-JsonArrayToMySql $room.facilities
        $imagesSql = Convert-JsonArrayToMySql $room.images
        $areaSql = if ($null -eq $room.area) { 'NULL' } else { $room.area }
        $enabledSql = if ($room.enabled) { '1' } else { '0' }
        "($numberSql, $nameSql, $($room.floor), $($room.price), $($room.capacity), $bedInfoSql, $areaSql, $facilitiesSql, $imagesSql, $descriptionSql, $enabledSql)"
    }
    $insertQuery = @"
START TRANSACTION;
INSERT INTO room (number, name, floor, price, capacity, bed_info, area, facilities, images, description, enabled)
VALUES $($valueRows -join ",`n")
ON DUPLICATE KEY UPDATE number = VALUES(number);
COMMIT;
"@
    $result = Invoke-MySqlQuery $TargetHost $TargetPort $TargetUser $TargetDatabase $targetPassword $insertQuery
    Write-Host "已导入 $($roomsToInsert.Count) 间客房；源库未修改，目标库同号记录保持原样。"
} finally {
    $targetPassword = $null
    $targetSecurePassword.Dispose()
}
