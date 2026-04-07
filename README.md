# ChatFabric Backend

Production-ready Spring Boot backend for a scalable real-time chat application using Java 8, Spring MVC, Spring Data JPA, MySQL, and STOMP over WebSocket.

## Tech Stack

- Java 8
- Spring Boot 2.7.18
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
├── controller
├── service
├── repository
├── entity
├── dto
├── config
├── websocket
├── exception
└── util
```

## Features

- User registration and user lookup
- Private chat creation between two users
- Message persistence in MySQL
- Fetch chat messages by chat ID
- STOMP messaging over `/ws`
- Topic subscriptions over `/topic/messages/{chatId}`
- In-memory online user tracking by default
- Redis-backed presence tracking when `PRESENCE_STORE=redis`

## API Summary

### User APIs

- `POST /api/users/register`
- `GET /api/users/{id}`

### Chat APIs

- `POST /api/chats`
- `GET /api/chats/{userId}`

### Message APIs

- `POST /api/messages`
- `GET /api/messages/{chatId}`

### WebSocket/STOMP

- Endpoint: `/ws`
- App destination prefix: `/app`
- Topic prefix: `/topic`
- Send destination: `/app/chat.sendMessage`
- Subscribe destination: `/topic/messages/{chatId}`

## Configuration

The app reads database and presence configuration from environment variables.

```powershell
$env:DB_HOST="localhost"
$env:DB_PORT="3306"
$env:DB_NAME="chatfabric"
$env:DB_USERNAME="root"
$env:DB_PASSWORD="root"
$env:PRESENCE_STORE="in-memory"
```

Optional Redis settings:

```powershell
$env:REDIS_HOST="localhost"
$env:REDIS_PORT="6379"
$env:PRESENCE_STORE="redis"
```

## Run Locally

Make sure Java 8 and Maven are available.

```powershell
$env:JAVA_HOME="C:\Program Files\OpenLogic\jdk-8.0.442.06-hotspot"
$env:Path="$env:JAVA_HOME\bin;$env:Path"
mvn spring-boot:run
```

By default the app starts on port `8080`.

## Step-by-Step Testing

The sequence below uses port `8080`. If you run the app on another port, replace `8080` consistently in both HTTP and WebSocket URLs.

### 1. Create user 1

Request:

```http
POST http://localhost:8080/api/users/register
Content-Type: application/json
```

Body:

```json
{
  "username": "alice",
  "password": "password123"
}
```

Example response:

```json
{
  "id": 1,
  "username": "alice",
  "status": "OFFLINE",
  "createdAt": "2026-04-07T13:00:00"
}
```

### 2. Create user 2

Request:

```http
POST http://localhost:8080/api/users/register
Content-Type: application/json
```

Body:

```json
{
  "username": "bob",
  "password": "password123"
}
```

### 3. Create a private chat

Request:

```http
POST http://localhost:8080/api/chats
Content-Type: application/json
```

Body:

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

### 4. Send a message by REST

Request:

```http
POST http://localhost:8080/api/messages
Content-Type: application/json
```

Body:

```json
{
  "chatId": 1,
  "senderId": 1,
  "content": "Hello Bob"
}
```

### 5. Fetch stored messages

Request:

```http
GET http://localhost:8080/api/messages/1
```

### 6. Test real-time messaging with the HTML client

Open:

```text
http://localhost:8080/chat-test.html
```

The page auto-detects the current backend host and port and fills the WebSocket URL automatically.

#### Alice tab

- `User ID Header`: `1`
- `Topic Destination`: `/topic/messages/1`
- Click `Connect`
- Click `Subscribe`

Payload:

```json
{
  "chatId": 1,
  "senderId": 1,
  "content": "Hello Bob, this is Alice"
}
```

Click `Send Message`.

#### Bob tab

Open a second browser tab with the same page.

- `User ID Header`: `2`
- `Topic Destination`: `/topic/messages/1`
- Click `Connect`
- Click `Subscribe`

Payload:

```json
{
  "chatId": 1,
  "senderId": 2,
  "content": "Hi Alice, I got your message"
}
```

Click `Send Message`.

### 7. Verify the chat history

Request:

```http
GET http://localhost:8080/api/messages/1
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

## Notes

- The backend validates that the sender belongs to the target chat.
- User presence flips to `ONLINE` on WebSocket connect and `OFFLINE` on disconnect.
- The HTML test client is available at `src/main/resources/static/chat-test.html`.
