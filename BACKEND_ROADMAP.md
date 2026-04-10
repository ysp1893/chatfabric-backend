# 🚀 Slack-like Chat System — Backend Roadmap

## 📌 Approach
This project follows a **strict backend-first, phase-wise execution strategy**.

- Each phase is independently implementable and testable
- No frontend dependency during backend phases
- Frontend (Angular) will be built only after each backend phase is completed
- APIs and WebSocket flows must be testable via Postman/WebSocket clients

---

## 🧱 Existing System

Current backend capabilities:

- Java 8 + Spring Boot
- REST APIs
- MySQL database
- WebSocket (STOMP)
  - Endpoint: `/ws`
- JWT authentication (REST + WebSocket)
- 1-to-1 chat
- Message persistence
- Basic presence tracking (in-memory / Redis optional)

---

# 🧭 Phase Breakdown

---

# 🟢 Phase 1 — Workspace Foundation

## 🎯 Objective
Introduce multi-tenant architecture using workspaces.

## ⚙️ Features
- Create workspace
- Join workspace
- Workspace membership management
- Role: ADMIN / MEMBER

## 🗄️ Database Changes
- `workspace`
  - id, name, created_by, created_at
- `workspace_members`
  - id, workspace_id, user_id, role

## 🔌 APIs
- `POST /api/workspaces` → Create workspace
- `GET /api/workspaces` → List user workspaces
- `POST /api/workspaces/{id}/invite` → Invite user
- `POST /api/workspaces/{id}/join` → Join workspace

## 🔄 WebSocket
- No major change (reuse existing infra)

## 🧠 Business Logic
- A user can belong to multiple workspaces
- Data isolation per workspace

## 🧪 Testing
- Create workspace via API
- Add users
- Verify isolation manually

## ✅ Output
- Multi-workspace system operational

---

# 🟡 Phase 2 — Channels & Group Chat

## 🎯 Objective
Enable Slack-like communication using channels and groups.

## ⚙️ Features
- Create channel (public/private)
- Add/remove members
- Group messaging
- Channel-based chat

## 🗄️ Database Changes
- Update `chat`
  - add: type (PRIVATE/GROUP/CHANNEL), workspace_id, name
- `chat_members`
  - chat_id, user_id

## 🔌 APIs
- `POST /api/channels`
- `GET /api/channels/{workspaceId}`
- `POST /api/channels/{id}/join`
- `POST /api/channels/{id}/leave`

## 🔄 WebSocket
- `/topic/messages/{chatId}` extended for group chats

## 🧠 Business Logic
- Public channels → open to workspace
- Private → invite-only

## 🧪 Testing
- Create channel
- Join/leave
- Send message to channel

## ✅ Output
- Group chat fully working

---

# 🔵 Phase 3 — Advanced Messaging

## 🎯 Objective
Enhance messaging experience

## ⚙️ Features
- Mentions (@user)
- Message metadata
- Reactions (👍 ❤️)

## 🗄️ Database Changes
- `message_reactions`
- Add metadata fields in `messages`

## 🔌 APIs
- `POST /api/messages/{id}/react`
- `GET /api/messages/{chatId}` (enhanced)

## 🔄 WebSocket
- Broadcast reaction updates

## 🧠 Business Logic
- Parse mentions
- Notify mentioned users

## 🧪 Testing
- Add reactions
- Mention users

## ✅ Output
- Rich messaging system

---

# 🟣 Phase 4 — Presence & Notifications

## 🎯 Objective
Improve real-time user awareness

## ⚙️ Features
- Online/offline tracking
- Last seen
- Notification events

## 🗄️ Database Changes
- Optional: persistent presence table

## 🔌 APIs
- `GET /api/presence`

## 🔄 WebSocket
- `/topic/presence`
- `/topic/notifications`

## 🧠 Business Logic
- Track active users
- Push events

## 🧪 Testing
- Login/logout
- Verify presence updates

## ✅ Output
- Live presence + notifications

---

# 🟠 Phase 5 — File Sharing

## 🎯 Objective
Support media and file exchange

## ⚙️ Features
- Upload file
- Send file in chat
- File metadata handling

## 🗄️ Database Changes
- `files`
  - id, url, type, size, uploaded_by

## 🔌 APIs
- `POST /api/files/upload`
- `GET /api/files/{id}`

## 🔄 WebSocket
- File message broadcast

## 🧠 Business Logic
- Store file
- Attach to message

## 🧪 Testing
- Upload + send file

## ✅ Output
- File sharing working

---

# 🔴 Phase 6 — Voice/Video Signaling

## 🎯 Objective
Enable real-time calling via WebRTC

## ⚙️ Features
- Call initiation
- Accept/reject call
- Exchange SDP/ICE

## 🗄️ Database Changes
- Optional: `call_sessions`

## 🔌 APIs
- Minimal (mostly WebSocket-driven)

## 🔄 WebSocket
- `/topic/call/{sessionId}`
- Events:
  - OFFER
  - ANSWER
  - ICE_CANDIDATE
  - CALL_END

## 🧠 Business Logic
- Backend acts as signaling server
- Media is peer-to-peer

## 🧪 Testing
- Simulate call signaling via WebSocket

## ✅ Output
- Call signaling ready

---

# ⚫ Phase 7 — Scalability Layer

## 🎯 Objective
Prepare system for high load

## ⚙️ Features
- Redis integration
- Pub/Sub messaging
- Distributed WebSocket handling

## 🗄️ Database Changes
- None

## 🔌 APIs
- No change

## 🔄 WebSocket
- Redis-backed broadcasting

## 🧠 Business Logic
- Scale horizontally

## 🧪 Testing
- Multi-instance testing

## ✅ Output
- Scalable backend

---

# 🔐 Phase 8 — Security Hardening

## 🎯 Objective
Ensure production-grade security

## ⚙️ Features
- Authorization checks
- Rate limiting
- Data validation

## 🗄️ Database Changes
- Optional audit logs

## 🔌 APIs
- Secured endpoints

## 🔄 WebSocket
- Secured subscriptions

## 🧠 Business Logic
- Enforce access control

## 🧪 Testing
- Unauthorized access attempts

## ✅ Output
- Secure backend

---

# 🧠 Architecture Overview


---

# ⚠️ Execution Strategy

1. Complete Phase 1 fully
2. Test using APIs/WebSocket
3. Move to next phase
4. Do NOT skip phases

---

# 🚀 Next Step

After reviewing this roadmap:

👉 Start with: `Phase 1 — Workspace Foundation`

Then proceed sequentially.
