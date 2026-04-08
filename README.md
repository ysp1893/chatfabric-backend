# ChatFabric Backend

Production-ready Spring Boot backend for a scalable real-time chat application using Java 8, Spring MVC, Spring Data JPA, MySQL, JWT-based authentication, and STOMP over WebSocket.

## Tech Stack

- Java 8
- Spring Boot 2.7.18
- Spring Security
- JWT authentication
- Maven
- MySQL
- Spring Data JPA
- Spring Web
- Spring WebSocket with STOMP
- Lombok
- Optional Redis-backed presence tracking

## Project Structure

```text
com.chatfabric.chat
|- controller
|- service
|- repository
|- entity
|- dto
|- config
|- websocket
|- security
|- exception
\- util
```

## Features

- User registration and login
- JWT-secured REST APIs
- JWT-secured STOMP WebSocket connections
- Private chat creation between two users
- Message persistence in MySQL
- Fetch chat messages by chat ID
- Dynamic user discovery through `GET /api/users`
- Realtime message delivery over `/topic/messages/{chatId}`
- Realtime presence delivery over `/topic/presence`
- In-memory online user tracking by default
- Redis-backed presence tracking when `PRESENCE_STORE=redis`
- Browser dashboard with auto-loaded chats, auto-create chat flow, presence indicators, and unread markers
- Configurable CORS origin allow-list
- Configurable HTTPS-only enforcement for production deployment
- In-memory per-minute API rate limiting
- Request logging and audit event logging
- Stronger username and payload validation
- E2EE-ready public key registration and lookup APIs
- Encrypted-only message creation for new traffic, while preserving legacy plaintext message reads

## API Summary

### Public APIs

- `POST /api/users/register`
- `POST /api/auth/login`

### Protected APIs

- `GET /api/users`
- `GET /api/users/{id}`
- `POST /api/keys/register`
- `GET /api/keys/{userId}/active`
- `POST /api/chats`
- `GET /api/chats/{userId}`
- `POST /api/messages`
- `GET /api/messages/{chatId}`

### WebSocket/STOMP

- Endpoint: `/ws`
- App destination prefix: `/app`
- Topic prefix: `/topic`
- Send destination: `/app/chat.sendMessage`
- Subscribe destination: `/topic/messages/{chatId}`
- Presence destination: `/topic/presence`
- STOMP connect header: `Authorization: Bearer <jwt>`

## Configuration

The app reads database, presence, and JWT configuration from environment variables.

```powershell
$env:DB_HOST="localhost"
$env:DB_PORT="3306"
$env:DB_NAME="chatfabric"
$env:DB_USERNAME="root"
$env:DB_PASSWORD="root"
$env:PRESENCE_STORE="in-memory"
$env:JWT_SECRET="chatfabric-super-secret-jwt-key-change-me-2026"
$env:JWT_EXPIRATION_SECONDS="900"
$env:REQUIRE_SSL="false"
$env:RATE_LIMIT_ENABLED="true"
$env:RATE_LIMIT_REQUESTS_PER_MINUTE="120"
```

Optional Redis settings:

```powershell
$env:REDIS_HOST="localhost"
$env:REDIS_PORT="6379"
$env:PRESENCE_STORE="redis"
```

Optional CORS origin allow-list overrides:

```powershell
$env:ALLOWED_ORIGIN_1="https://your-frontend.example.com"
$env:ALLOWED_ORIGIN_2="https://admin.example.com"
```

## Stage B Hardening

The backend now includes these production-hardening controls:

- CORS is restricted to configured origins instead of wildcard access
- HTTPS-only mode can be enforced with `REQUIRE_SSL=true`
- API rate limiting is enabled by default
- request logging records method, path, status, duration, principal, and client address
- audit logging records authentication, chat access, and message send events
- validation is stricter for usernames and request payloads
- application log level is reduced from `DEBUG` to `INFO` by default

### Notes on TLS

TLS certificates and reverse-proxy termination are deployment concerns, so this project provides app-side enforcement rather than shipping certificates directly. In production, deploy behind HTTPS and enable:

```powershell
$env:REQUIRE_SSL="true"
```

If you run behind a proxy or load balancer, forward the standard proxy headers so Spring Security can correctly detect secure requests.

## Run Locally

Make sure Java 8 and Maven are available.

```powershell
$env:JAVA_HOME="C:\Program Files\OpenLogic\jdk-8.0.442.06-hotspot"
$env:Path="$env:JAVA_HOME\bin;$env:Path"
mvn spring-boot:run
```

By default the app starts on port `8080`.

## Step-by-Step Secure Testing

The sequence below uses port `8080`. If you run the app on another port, replace `8080` consistently in both HTTP and WebSocket URLs.

### 1. Register Alice

```http
POST http://localhost:8080/api/users/register
Content-Type: application/json
```

```json
{
  "username": "alice",
  "password": "password123"
}
```

### 2. Register Bob

```http
POST http://localhost:8080/api/users/register
Content-Type: application/json
```

```json
{
  "username": "bob",
  "password": "password123"
}
```

### 3. Login as Alice and get a JWT

```http
POST http://localhost:8080/api/auth/login
Content-Type: application/json
```

```json
{
  "username": "alice",
  "password": "password123"
}
```

Example response:

```json
{
  "tokenType": "Bearer",
  "accessToken": "<alice-jwt>",
  "expiresInSeconds": 900,
  "userId": 1,
  "username": "alice"
}
```

### 4. Login as Bob and get a JWT

```http
POST http://localhost:8080/api/auth/login
Content-Type: application/json
```

```json
{
  "username": "bob",
  "password": "password123"
}
```

### 5. Create a private chat as Alice

```http
POST http://localhost:8080/api/chats
Authorization: Bearer <alice-jwt>
Content-Type: application/json
```

```json
{
  "firstUserId": 1,
  "secondUserId": 2
}
```

Example response:

```json
{
  "id": 1,
  "type": "PRIVATE",
  "createdAt": "2026-04-07T13:01:00",
  "participants": [
    {
      "userId": 1,
      "username": "alice",
      "status": "OFFLINE"
    },
    {
      "userId": 2,
      "username": "bob",
      "status": "OFFLINE"
    }
  ]
}
```

Use the returned `id` as your `chatId`.

### 6. Send a message by REST as Alice

```http
POST http://localhost:8080/api/messages
Authorization: Bearer <alice-jwt>
Content-Type: application/json
```

```json
{
  "chatId": 1,
  "content": "Hello Bob"
}
```

The sender is inferred from the JWT. Do not send `senderId`.

### 7. Fetch stored messages as Alice or Bob

```http
GET http://localhost:8080/api/messages/1
Authorization: Bearer <alice-jwt>
```

### 8. Open the browser dashboard

Open:

```text
http://localhost:8080/chat-test.html
```

The page auto-detects the current backend host and port, lets you log in, stores the returned JWT in the page, and uses that token in the STOMP `Authorization` header automatically.

### 9. Login in the browser dashboard

Use one browser or tab per user.

#### Alice tab

- Username: `alice`
- Password: `password123`
- Click `Login`

After login the dashboard:

- loads all chats for the signed-in user
- loads all users in the system
- auto-connects realtime
- subscribes to presence updates
- updates online and offline status automatically

#### Bob tab

Open a second browser tab with the same page.

- Username: `bob`
- Password: `password123`
- Click `Login`

### 10. Start a chat dynamically

Inside the dashboard:

1. Look at `Available Users`
2. Click a user card
3. If a chat already exists, it opens automatically
4. If no chat exists, a private chat is created automatically and opened

No manual `chatId` or `userId` entry is required in the dashboard.

### 11. Send messages from the dashboard

When a chat is selected:

1. Type in the message box
2. Click `Send`
3. The message is sent through `/app/chat.sendMessage`
4. The active conversation refreshes in realtime

### 12. Verify presence updates

When multiple users are logged into the dashboard:

- users switching online/offline should update automatically in:
  - `Available Users`
  - `Chats`

This is delivered through `/topic/presence`.

### 13. Verify unread indicators

If a message arrives for a chat that is not currently open:

- the related chat card shows a `NEW <count>` badge
- opening that chat resets the unread count

### 14. Verify the chat history by REST

```http
GET http://localhost:8080/api/messages/1
Authorization: Bearer <bob-jwt>
```

Expected response:

```json
[
  {
    "id": 1,
    "chatId": 1,
    "senderId": 1,
    "senderUsername": "alice",
    "content": "Hello Bob, this is Alice",
    "timestamp": "2026-04-07T13:04:40.055",
    "status": "SENT"
  },
  {
    "id": 2,
    "chatId": 1,
    "senderId": 2,
    "senderUsername": "bob",
    "content": "Hi Alice, I got your message",
    "timestamp": "2026-04-07T13:05:02.354",
    "status": "SENT"
  }
]
```

## Security Notes

- Passwords are hashed with BCrypt.
- All chat and message endpoints now require a valid JWT.
- The backend derives message sender identity from the authenticated token, not from the request body.
- WebSocket presence tracking now uses the authenticated STOMP principal.
- WebSocket presence changes are broadcast to all connected dashboards.
- API requests are rate limited by default to reduce brute-force and abuse risk.
- CORS is origin-restricted through configuration.
- Audit logs record auth success/failure and chat/message actions.
- Replace the default `JWT_SECRET` in real environments.

## Stage C1: E2EE-Ready Backend

The backend now includes the first practical E2EE preparation layer while keeping existing plaintext chat flows working.

### What Stage C1 Adds

- `UserKey` storage for public encryption and signing keys
- public key registration endpoint for the authenticated user
- public key lookup endpoint for any user
- message schema support for encrypted payload metadata
- dual message formats:
  - `PLAINTEXT_V1`
  - `E2EE_V1`

### Public Key APIs

Register or rotate the active key for the authenticated user:

```http
POST http://localhost:8080/api/keys/register
Authorization: Bearer <jwt>
Content-Type: application/json
```

```json
{
  "publicEncryptionKey": "base64-or-pem-value",
  "publicSigningKey": "base64-or-pem-value",
  "keyVersion": 1
}
```

Fetch the active public key for a user:

```http
GET http://localhost:8080/api/keys/2/active
Authorization: Bearer <jwt>
```

### Message Formats

Legacy plaintext messages still work:

```json
{
  "chatId": 1,
  "content": "Hello Bob"
}
```

The backend now also accepts encrypted payload packages:

```json
{
  "chatId": 1,
  "messageFormat": "E2EE_V1",
  "ciphertext": "base64-ciphertext",
  "nonce": "base64-nonce",
  "algorithm": "AES_GCM",
  "encryptedMessageKey": "base64-wrapped-key",
  "signature": "base64-signature",
  "keyVersion": 1
}
```

For `E2EE_V1`, the backend validates that:

- `ciphertext` is present
- `nonce` is present
- `algorithm` is present
- `encryptedMessageKey` is present
- `signature` is present
- `keyVersion` is valid

The backend stores these encrypted fields but does not decrypt them.

### Current Scope

Stage C1 is backend preparation only.

It does **not** yet include:

- browser-side key generation
- client-side encryption/decryption
- client-side signature verification
- encrypted-only UI flows

Those belong to the next step after C1.

## Stage C2: Browser Client Crypto Flow

The browser test client now includes a practical E2EE prototype built on top of the C1 backend contracts.

### What Stage C2 Adds

- browser-managed encryption and signing key pairs
- automatic public key registration after login
- local private key storage in browser `localStorage`
- encrypted send flow from the browser dashboard
- local decryption when messages are fetched or received
- client-side signature verification display

### Browser Crypto Model

The current prototype uses:

- `RSA-OAEP (2048, SHA-256)` to wrap the per-message AES key for each participant
- `AES-GCM (256-bit)` to encrypt message content
- `ECDSA P-256` to sign the encrypted payload

For each outgoing message, the browser:

1. generates a fresh AES-GCM key
2. encrypts the plaintext locally
3. wraps that AES key for both sender and recipient
4. signs the encrypted payload
5. sends only the encrypted package to the backend

The backend still stores and forwards the message, but it does not decrypt it.

### Browser Key Behavior

After login, `chat-test.html` will:

1. check whether the current user already has an active public key on the backend
2. check whether the matching private key exists in the current browser
3. reuse that local key pair when possible
4. register a new key version automatically when the browser has no matching private key

Important:

- this is a single-browser prototype
- if you clear browser storage, old encrypted messages tied to the removed private key may no longer decrypt in that browser
- key history lookup by version is not implemented yet, only the active key endpoint is used

### How To Test Stage C2

1. Start the backend and open:

```text
http://localhost:8080/chat-test.html
```

2. Log in as one user, for example `alice`

3. Confirm the left panel shows:

- `E2EE READY`
- a key-version message in the crypto summary

4. Log in from another browser or incognito window as `bob`

5. Open or create a chat between the two users

6. Send a message from one browser tab

Expected:

- the outgoing payload is encrypted before send
- the receiving browser decrypts the message locally
- the message panel shows an `Encrypted` status line
- the status line also shows whether the signature verified

7. Refresh the selected chat

Expected:

- encrypted messages remain readable in the same browser that still holds the private key

### Current Stage C2 Limitations

- this is still a prototype browser client, not a production key-management system
- private keys are stored in browser local storage for demo purposes
- key history retrieval by version is not available yet
- multi-device support is not available yet
- existing legacy plaintext messages still render as `Legacy plaintext`

## Stage C3: Encrypted-Only Messaging

The system now moves from dual-mode message creation to encrypted-only creation for all new traffic.

### What Stage C3 Changes

- the backend no longer accepts new `PLAINTEXT_V1` message creation
- any send request containing plaintext `content` is rejected
- new messages must be sent as `E2EE_V1`
- historical plaintext messages remain readable for migration purposes

### New Send Rule

The following send payload is now rejected:

```json
{
  "chatId": 1,
  "content": "Hello Bob"
}
```

The backend now requires an encrypted payload package for every new message:

```json
{
  "chatId": 1,
  "messageFormat": "E2EE_V1",
  "ciphertext": "base64-ciphertext",
  "nonce": "base64-nonce",
  "algorithm": "AES-GCM/RSA-OAEP-256/ECDSA-P256",
  "encryptedMessageKey": "base64-wrapped-key-package",
  "signature": "base64-signature",
  "keyVersion": 1
}
```

If plaintext content is submitted, the API responds with a bad-request error explaining that plaintext creation is disabled and the client must encrypt before send.

### How To Verify Stage C3

1. Open [http://localhost:8080/chat-test.html](http://localhost:8080/chat-test.html)
2. Log in as two users in separate browser sessions
3. Send a message from the dashboard
4. Confirm the message still arrives and decrypts in the recipient browser
5. Try posting a plaintext REST message manually

Example plaintext request that should now fail:

```http
POST http://localhost:8080/api/messages
Authorization: Bearer <jwt>
Content-Type: application/json
```

```json
{
  "chatId": 1,
  "content": "This should fail now"
}
```

Expected result:

- HTTP `400 Bad Request`
- error message indicating plaintext creation is disabled

## HTML Test Client

The browser test client lives at:

`src/main/resources/static/chat-test.html`

It is intended for local validation of:

- login
- token acquisition
- dynamic chat loading
- available user discovery
- auto-create private chat flow
- realtime message send/receive
- realtime presence updates
- unread indicators for inactive chats
- automatic browser key generation and registration
- browser-side encrypted messaging
- local decryption and signature verification display
