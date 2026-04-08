param(
    [string]$BaseUrl = "http://localhost:8080"
)

$ErrorActionPreference = "Stop"

function Write-Step($message) {
    Write-Host ""
    Write-Host "=== $message ===" -ForegroundColor Cyan
}

function Write-Pass($message) {
    Write-Host "PASS: $message" -ForegroundColor Green
}

function Fail($message) {
    throw $message
}

function Assert-True($condition, $message) {
    if (-not $condition) {
        Fail $message
    }
    Write-Pass $message
}

function Assert-Equal($expected, $actual, $message) {
    if ($expected -ne $actual) {
        Fail "$message (expected '$expected', got '$actual')"
    }
    Write-Pass $message
}

function New-RandomSuffix {
    return ([DateTimeOffset]::UtcNow.ToUnixTimeMilliseconds().ToString() + "-" + (Get-Random -Minimum 1000 -Maximum 9999))
}

function Read-ErrorBody($errorRecord) {
    if ($errorRecord.Exception.Response -and $errorRecord.Exception.Response.GetResponseStream()) {
        $reader = New-Object System.IO.StreamReader($errorRecord.Exception.Response.GetResponseStream())
        return $reader.ReadToEnd()
    }
    return $errorRecord.Exception.Message
}

function Invoke-Json {
    param(
        [string]$Method,
        [string]$Uri,
        [hashtable]$Headers = $null,
        [object]$Body = $null
    )

    $params = @{
        Method = $Method
        Uri = $Uri
        ContentType = "application/json"
    }

    if ($Headers) {
        $params.Headers = $Headers
    }

    if ($null -ne $Body) {
        $params.Body = ($Body | ConvertTo-Json -Depth 10 -Compress)
    }

    return Invoke-RestMethod @params
}

function Invoke-ExpectStatus {
    param(
        [string]$Method,
        [string]$Uri,
        [int]$ExpectedStatus,
        [hashtable]$Headers = $null,
        [object]$Body = $null
    )

    try {
        $params = @{
            Method = $Method
            Uri = $Uri
            ContentType = "application/json"
        }
        if ($Headers) {
            $params.Headers = $Headers
        }
        if ($null -ne $Body) {
            $params.Body = ($Body | ConvertTo-Json -Depth 10 -Compress)
        }
        Invoke-WebRequest -UseBasicParsing @params | Out-Null
        $actual = 200
    } catch {
        if ($_.Exception.Response) {
            $actual = [int]$_.Exception.Response.StatusCode
        } else {
            throw
        }
    }

    Assert-Equal $ExpectedStatus $actual "$Method $Uri returned expected status"
}

function New-FakeBase64($label) {
    $value = "$label-" + [Guid]::NewGuid().ToString("N")
    return [Convert]::ToBase64String([Text.Encoding]::UTF8.GetBytes($value))
}

function New-E2eePayload {
    param(
        [long]$ChatId,
        [int]$KeyVersion = 1
    )

    return @{
        chatId = $ChatId
        messageFormat = "E2EE_V1"
        ciphertext = New-FakeBase64 "ciphertext"
        nonce = New-FakeBase64 "nonce"
        algorithm = "AES-GCM/RSA-OAEP-256/ECDSA-P256"
        encryptedMessageKey = New-FakeBase64 "wrapped-key"
        signature = New-FakeBase64 "signature"
        keyVersion = $KeyVersion
    }
}

function New-StompClient {
    param(
        [string]$Name,
        [string]$WsUrl,
        [string]$Token
    )

    $socket = [System.Net.WebSockets.ClientWebSocket]::new()
    $socket.Options.KeepAliveInterval = [TimeSpan]::FromSeconds(20)
    $socket.ConnectAsync([Uri]$WsUrl, [Threading.CancellationToken]::None).GetAwaiter().GetResult()

    $client = @{
        Name = $Name
        Socket = $socket
        Buffer = ""
        Host = ([Uri]$WsUrl).Host
    }

    Send-StompFrame -Client $client -Command "CONNECT" -Headers @{
        "accept-version" = "1.2"
        "heart-beat" = "0,0"
        "host" = $client.Host
        "Authorization" = "Bearer $Token"
    }

    $frame = Receive-StompFrame -Client $client -TimeoutSeconds 10
    Assert-Equal "CONNECTED" $frame.Command "$Name connected over STOMP"
    return $client
}

function Send-StompFrame {
    param(
        [hashtable]$Client,
        [string]$Command,
        [hashtable]$Headers = $null,
        [string]$Body = ""
    )

    $lines = New-Object System.Collections.Generic.List[string]
    $lines.Add($Command)
    if ($Headers) {
        foreach ($key in $Headers.Keys) {
            $lines.Add($key + ":" + $Headers[$key])
        }
    }

    $payload = ($lines -join "`n") + "`n`n"
    if ($Body) {
        $payload += $Body
    }
    $payload += [char]0

    $bytes = [Text.Encoding]::UTF8.GetBytes($payload)
    $segment = [System.ArraySegment[byte]]::new($bytes)
    $Client.Socket.SendAsync($segment, [System.Net.WebSockets.WebSocketMessageType]::Text, $true, [Threading.CancellationToken]::None).GetAwaiter().GetResult()
}

function Parse-StompFrame {
    param([string]$RawFrame)

    $normalized = $RawFrame -replace "`r", ""
    $splitIndex = $normalized.IndexOf("`n`n")
    if ($splitIndex -ge 0) {
        $headerText = $normalized.Substring(0, $splitIndex)
        $body = $normalized.Substring($splitIndex + 2)
    } else {
        $headerText = $normalized
        $body = ""
    }

    $lines = $headerText -split "`n"
    $headers = @{}
    for ($i = 1; $i -lt $lines.Length; $i++) {
        if ([string]::IsNullOrWhiteSpace($lines[$i])) {
            continue
        }
        $parts = $lines[$i] -split ":", 2
        if ($parts.Length -eq 2) {
            $headers[$parts[0]] = $parts[1]
        }
    }

    return @{
        Command = $lines[0].Trim()
        Headers = $headers
        Body = $body
        Raw = $normalized
    }
}

function Receive-StompFrame {
    param(
        [hashtable]$Client,
        [int]$TimeoutSeconds = 10
    )

    $deadline = (Get-Date).AddSeconds($TimeoutSeconds)
    while ((Get-Date) -lt $deadline) {
        $nullIndex = $Client.Buffer.IndexOf([char]0)
        if ($nullIndex -ge 0) {
            $rawFrame = $Client.Buffer.Substring(0, $nullIndex)
            $Client.Buffer = $Client.Buffer.Substring($nullIndex + 1)
            return Parse-StompFrame -RawFrame $rawFrame
        }

        $buffer = New-Object byte[] 4096
        $segment = [System.ArraySegment[byte]]::new($buffer)
        $cts = [Threading.CancellationTokenSource]::new([TimeSpan]::FromMilliseconds(750))
        try {
            $result = $Client.Socket.ReceiveAsync($segment, $cts.Token).GetAwaiter().GetResult()
        } catch [System.OperationCanceledException] {
            continue
        }

        if ($result.MessageType -eq [System.Net.WebSockets.WebSocketMessageType]::Close) {
            Fail "$($Client.Name) websocket closed while waiting for a STOMP frame"
        }

        if ($result.Count -gt 0) {
            $Client.Buffer += [Text.Encoding]::UTF8.GetString($buffer, 0, $result.Count)
        }
    }

    Fail "Timed out waiting for a STOMP frame for $($Client.Name)"
}

function Wait-ForStompFrame {
    param(
        [hashtable]$Client,
        [scriptblock]$Predicate,
        [int]$TimeoutSeconds = 10
    )

    $deadline = (Get-Date).AddSeconds($TimeoutSeconds)
    while ((Get-Date) -lt $deadline) {
        $remaining = [Math]::Max(1, [int](($deadline - (Get-Date)).TotalSeconds))
        $frame = Receive-StompFrame -Client $Client -TimeoutSeconds $remaining
        if (& $Predicate $frame) {
            return $frame
        }
    }

    Fail "Timed out waiting for a matching STOMP frame for $($Client.Name)"
}

function Close-StompClient {
    param([hashtable]$Client)

    if (-not $Client -or -not $Client.Socket) {
        return
    }

    try {
        if ($Client.Socket.State -eq [System.Net.WebSockets.WebSocketState]::Open) {
            Send-StompFrame -Client $Client -Command "DISCONNECT"
            $Client.Socket.CloseAsync([System.Net.WebSockets.WebSocketCloseStatus]::NormalClosure, "done", [Threading.CancellationToken]::None).GetAwaiter().GetResult()
        }
    } catch {
        try {
            $Client.Socket.Dispose()
        } catch {
        }
    }
}

$wsUrl = $BaseUrl.Replace("https://", "wss://").Replace("http://", "ws://") + "/ws"
$suffix = New-RandomSuffix
$aliceUsername = "smoke_alice_$suffix"
$bobUsername = "smoke_bob_$suffix"
$password = "password123"
$aliceWs = $null
$bobWs = $null

try {
    Write-Step "Anonymous protection"
    Invoke-ExpectStatus -Method "GET" -Uri "$BaseUrl/api/users" -ExpectedStatus 401

    Write-Step "User registration"
    $alice = Invoke-Json -Method "POST" -Uri "$BaseUrl/api/users/register" -Body @{
        username = $aliceUsername
        password = $password
    }
    $bob = Invoke-Json -Method "POST" -Uri "$BaseUrl/api/users/register" -Body @{
        username = $bobUsername
        password = $password
    }
    Assert-True ($alice.id -gt 0) "Alice user was created"
    Assert-True ($bob.id -gt 0) "Bob user was created"

    Write-Step "Login"
    $aliceLogin = Invoke-Json -Method "POST" -Uri "$BaseUrl/api/auth/login" -Body @{
        username = $aliceUsername
        password = $password
    }
    $bobLogin = Invoke-Json -Method "POST" -Uri "$BaseUrl/api/auth/login" -Body @{
        username = $bobUsername
        password = $password
    }
    $aliceHeaders = @{ Authorization = "Bearer $($aliceLogin.accessToken)" }
    $bobHeaders = @{ Authorization = "Bearer $($bobLogin.accessToken)" }
    Assert-Equal $aliceUsername $aliceLogin.username "Alice login returned expected username"
    Assert-Equal $bobUsername $bobLogin.username "Bob login returned expected username"

    Write-Step "Protected user APIs"
    $users = Invoke-Json -Method "GET" -Uri "$BaseUrl/api/users" -Headers $aliceHeaders
    Assert-True (($users | Where-Object { $_.username -eq $aliceUsername }).Count -eq 1) "Alice appears in authenticated user list"
    Assert-True (($users | Where-Object { $_.username -eq $bobUsername }).Count -eq 1) "Bob appears in authenticated user list"
    $aliceProfile = Invoke-Json -Method "GET" -Uri "$BaseUrl/api/users/$($alice.id)" -Headers $aliceHeaders
    Assert-Equal $aliceUsername $aliceProfile.username "Alice can fetch her own profile"

    Write-Step "Create private chat"
    $chat = Invoke-Json -Method "POST" -Uri "$BaseUrl/api/chats" -Headers $aliceHeaders -Body @{
        firstUserId = $alice.id
        secondUserId = $bob.id
    }
    Assert-True ($chat.id -gt 0) "Private chat was created"
    $aliceChats = Invoke-Json -Method "GET" -Uri "$BaseUrl/api/chats/$($alice.id)" -Headers $aliceHeaders
    $bobChats = Invoke-Json -Method "GET" -Uri "$BaseUrl/api/chats/$($bob.id)" -Headers $bobHeaders
    Assert-True (($aliceChats | Where-Object { $_.id -eq $chat.id }).Count -eq 1) "Alice can fetch the chat"
    Assert-True (($bobChats | Where-Object { $_.id -eq $chat.id }).Count -eq 1) "Bob can fetch the chat"

    Write-Step "Register and fetch public keys"
    $aliceKey = Invoke-Json -Method "POST" -Uri "$BaseUrl/api/keys/register" -Headers $aliceHeaders -Body @{
        publicEncryptionKey = New-FakeBase64 "alice-pub-enc"
        publicSigningKey = New-FakeBase64 "alice-pub-sign"
        keyVersion = 1
    }
    $bobKey = Invoke-Json -Method "POST" -Uri "$BaseUrl/api/keys/register" -Headers $bobHeaders -Body @{
        publicEncryptionKey = New-FakeBase64 "bob-pub-enc"
        publicSigningKey = New-FakeBase64 "bob-pub-sign"
        keyVersion = 1
    }
    Assert-Equal 1 $aliceKey.keyVersion "Alice key registered as version 1"
    Assert-Equal 1 $bobKey.keyVersion "Bob key registered as version 1"
    $aliceActiveKey = Invoke-Json -Method "GET" -Uri "$BaseUrl/api/keys/$($alice.id)/active" -Headers $bobHeaders
    $bobActiveKey = Invoke-Json -Method "GET" -Uri "$BaseUrl/api/keys/$($bob.id)/active" -Headers $aliceHeaders
    Assert-Equal 1 $aliceActiveKey.keyVersion "Alice active key is retrievable"
    Assert-Equal 1 $bobActiveKey.keyVersion "Bob active key is retrievable"

    Write-Step "Encrypted-only enforcement"
    Invoke-ExpectStatus -Method "POST" -Uri "$BaseUrl/api/messages" -ExpectedStatus 400 -Headers $aliceHeaders -Body @{
        chatId = $chat.id
        content = "This plaintext request should fail"
    }

    Write-Step "Encrypted REST messaging"
    $restMessage = Invoke-Json -Method "POST" -Uri "$BaseUrl/api/messages" -Headers $aliceHeaders -Body (New-E2eePayload -ChatId $chat.id -KeyVersion 1)
    Assert-Equal "E2EE_V1" $restMessage.messageFormat "REST message stored as E2EE_V1"
    $messagesAfterRest = Invoke-Json -Method "GET" -Uri "$BaseUrl/api/messages/$($chat.id)" -Headers $aliceHeaders
    Assert-True ($messagesAfterRest.Count -ge 1) "Messages endpoint returns encrypted history"

    Write-Step "Realtime STOMP flow"
    $aliceWs = New-StompClient -Name "Alice" -WsUrl $wsUrl -Token $aliceLogin.accessToken
    Send-StompFrame -Client $aliceWs -Command "SUBSCRIBE" -Headers @{ id = "presence-sub"; destination = "/topic/presence" }
    Send-StompFrame -Client $aliceWs -Command "SUBSCRIBE" -Headers @{ id = "chat-sub"; destination = "/topic/messages/$($chat.id)" }

    $bobWs = New-StompClient -Name "Bob" -WsUrl $wsUrl -Token $bobLogin.accessToken
    $presenceOnline = Wait-ForStompFrame -Client $aliceWs -TimeoutSeconds 10 -Predicate {
        param($frame)
        if ($frame.Command -ne "MESSAGE") { return $false }
        if ($frame.Headers.destination -ne "/topic/presence") { return $false }
        $body = $frame.Body | ConvertFrom-Json
        return ($body.userId -eq $bob.id -and $body.status -eq "ONLINE")
    }
    $presenceOnlineBody = $presenceOnline.Body | ConvertFrom-Json
    Assert-Equal "ONLINE" $presenceOnlineBody.status "Alice received Bob's ONLINE presence update"

    $stompPayload = New-E2eePayload -ChatId $chat.id -KeyVersion 1
    Send-StompFrame -Client $bobWs -Command "SEND" -Headers @{
        destination = "/app/chat.sendMessage"
        "content-type" = "application/json"
    } -Body (($stompPayload | ConvertTo-Json -Depth 10 -Compress))

    $chatBroadcast = Wait-ForStompFrame -Client $aliceWs -TimeoutSeconds 10 -Predicate {
        param($frame)
        if ($frame.Command -ne "MESSAGE") { return $false }
        if ($frame.Headers.destination -ne "/topic/messages/$($chat.id)") { return $false }
        $body = $frame.Body | ConvertFrom-Json
        return ($body.chatId -eq $chat.id -and $body.senderId -eq $bob.id -and $body.messageFormat -eq "E2EE_V1")
    }
    $chatBroadcastBody = $chatBroadcast.Body | ConvertFrom-Json
    Assert-Equal "E2EE_V1" $chatBroadcastBody.messageFormat "Alice received Bob's encrypted STOMP message"

    Close-StompClient -Client $bobWs
    $bobWs = $null
    $presenceOffline = Wait-ForStompFrame -Client $aliceWs -TimeoutSeconds 10 -Predicate {
        param($frame)
        if ($frame.Command -ne "MESSAGE") { return $false }
        if ($frame.Headers.destination -ne "/topic/presence") { return $false }
        $body = $frame.Body | ConvertFrom-Json
        return ($body.userId -eq $bob.id -and $body.status -eq "OFFLINE")
    }
    $presenceOfflineBody = $presenceOffline.Body | ConvertFrom-Json
    Assert-Equal "OFFLINE" $presenceOfflineBody.status "Alice received Bob's OFFLINE presence update"

    Write-Step "Final history check"
    $messagesAfterWs = Invoke-Json -Method "GET" -Uri "$BaseUrl/api/messages/$($chat.id)" -Headers $aliceHeaders
    Assert-True ($messagesAfterWs.Count -ge 2) "Encrypted REST and STOMP messages are both stored"

    Write-Host ""
    Write-Host "Smoke test completed successfully." -ForegroundColor Green
    Write-Host "Created users: $aliceUsername, $bobUsername"
    Write-Host "Chat ID: $($chat.id)"
} catch {
    Write-Host ""
    Write-Host "Smoke test failed." -ForegroundColor Red
    Write-Host $_.Exception.Message -ForegroundColor Red
    if ($_.InvocationInfo) {
        Write-Host ("At: " + $_.InvocationInfo.PositionMessage) -ForegroundColor DarkRed
    }
    exit 1
} finally {
    Close-StompClient -Client $bobWs
    Close-StompClient -Client $aliceWs
}
