# HWX SMS Gateway — Backend Server

A production-oriented, modular, multi-user Android SMS Gateway backend server engineered with Node.js, TypeScript, Express, Firebase Admin SDK, Cloud Firestore, and real-time WebSockets.

---

## 1. System Architecture Overview

```
                                      +---------------------------------------------+
                                      |            API Consumers / Clients          |
                                      |  (Web Dashboard, CRM, E-commerce, Node SDK) |
                                      +---------------------------------------------+
                                                             |
                                           HTTPS REST API    |   Bearer Firebase ID Token OR
                                                             |   X-API-Key: hwx_live_...
                                                             v
                                      +---------------------------------------------+
                                      |          HWX Express Backend Server         |
                                      |  * Security & Auth (Firebase Admin & HMAC)  |
                                      |  * Rate Limiting (express-rate-limit)       |
                                      |  * Zod Request Payload Validation           |
                                      |  * Dual-SIM & Bot Routing Engine            |
                                      |  * SSRF-Protected Webhook Dispatcher        |
                                      |  * Swagger 3.0 Interactive OpenAPI (/docs)  |
                                      +---------------------------------------------+
                                            |                                |
                                            | Firestore                      | WebSocket (/ws/device)
                                            v Persistent Jobs & Subcollections | Two-way Real-Time Link
                        +----------------------------+             +-----------------------------+
                        |      Cloud Firestore       |             |   Android Gateway Devices   |
                        | users/{uid}                |             |  (HWX Android Client App)   |
                        | ├── apiKeys/               |             |  * Dual-SIM slot management |
                        | ├── devices/               |             |  * Outbound SMS dispatch    |
                        | │   └── sims/              |             |  * Inbound SMS forwarding   |
                        | ├── bots/                  |             |  * Delivery status callbacks|
                        | ├── smsJobs/               |             +-----------------------------+
                        | ├── smsMessages/           |
                        | └── webhooks/              |
                        +----------------------------+
```

---

## 2. Security Architecture

- **Single Firebase Admin Environment Variable**: All credentials reside in `FIREBASE_SERVICE_ACCOUNT` as a single JSON string. Individual private keys or client emails are never leaked, logged, or hardcoded.
- **Hashed API Keys**: Only cryptographic SHA-256 hashes (`keyHash`) are stored in Firestore. Raw keys with prefix `hwx_live_` are revealed exactly once upon creation.
- **Device Authentication**: Android devices authenticate with high-entropy device tokens (`hwx_dev_...`) stored as SHA-256 hashes.
- **SSRF Defense with DNS Resolution**: All user-provided webhook URLs undergo protocol checks, DNS resolution, and verification against RFC 1918 private IP ranges, link-local metadata addresses (`169.254.169.254`, `metadata.google.internal`), loopback addresses (`127.0.0.1`), and multicast ranges.
- **HMAC-SHA256 Webhook Signatures**: All outbound webhook payloads include cryptographic signatures in `X-HWX-Signature` headers with timing-safe verification.
- **Idempotency Protection**: SMS sending supports `Idempotency-Key` headers to prevent accidental duplicate SMS dispatches.
- **Strict User Isolation**: All Firestore database operations are isolated per user under `users/{uid}/*`.

---

## 3. Dual-SIM Management & Bot Routing Rules

1. **Bot Fixed SIM**: The bot sends strictly through its pre-assigned SIM card slot. If the SIM is removed or carrier changed, the job is blocked until verified.
2. **Auto Switch**: The bot uses its primary SIM by default. If the primary SIM is exhausted or offline, it automatically falls back **only** to explicitly authorized fallback SIMs.
3. **Manual SIM**: The API consumer must supply an explicit `sim_id` in the request body.
4. **Ambiguity Gate**: If SIM identity is ambiguous, sending is blocked and returns an explicit `SIM_AMBIGUOUS` error.
5. **Reply Association**: Inbound SMS messages are tagged with the receiving SIM slot. Outbound replies default to the receiving SIM slot.

---

## 4. Local Development & Installation

### Prerequisites
- Node.js 20+ LTS or 22+ LTS
- A Firebase project with Firestore and Authentication enabled (Project ID: `hwxgetway`)

### Installation Steps

1. **Clone the repository and install dependencies**:
   ```bash
   git clone https://github.com/your-org/hwx-sms-gateway.git
   cd hwx-sms-gateway
   npm install
   ```

2. **Configure Environment Variables**:
   Copy the sample environment file:
   ```bash
   cp .env.example .env
   ```

   Edit `.env` and configure your credentials:
   ```env
   PORT=3000
   NODE_ENV=development
   FIREBASE_SERVICE_ACCOUNT='{"type":"service_account","project_id":"hwxgetway","private_key_id":"...","private_key":"[REDACTED-SECRET]\n","client_email":"firebase-adminsdk-xxxxx@hwxgetway.iam.gserviceaccount.com","client_id":"...","token_uri":"https://oauth2.googleapis.com/token"}'
   WEBHOOK_TIMEOUT_MS=10000
   MAX_SMS_PER_MINUTE=60
   ```

3. **Start the Development Server**:
   ```bash
   npm run dev
   ```

   The server will start listening on port 3000:
   - **Swagger OpenAPI Docs**: `http://localhost:3000/docs`
   - **Health Endpoint**: `http://localhost:3000/health`
   - **WebSocket Gateway**: `ws://localhost:3000/ws/device`

---

## 5. Running Tests

Unit and integration tests run with mock Firebase credentials and do not require live cloud credentials:

```bash
npm test
```

To run individual test suites:
```bash
npx jest tests/unit/security.test.ts
npx jest tests/unit/simSelection.test.ts
npx jest tests/integration/api.test.ts
```

---

## 6. REST API Reference & Examples

### Health Check
```bash
curl -X GET http://localhost:3000/health
```

### User Profile (`GET /v1/auth/me`)
```bash
curl -X GET http://localhost:3000/v1/auth/me \
  -H "Authorization: Bearer <FIREBASE_ID_TOKEN>"
```

### Create API Key (`POST /v1/api-keys`)
```bash
curl -X POST http://localhost:3000/v1/api-keys \
  -H "Authorization: Bearer <FIREBASE_ID_TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Production Server Key"
  }'
```

*Response (Raw key shown once)*:
```json
{
  "success": true,
  "data": {
    "id": "e91024cd-...",
    "name": "Production Server Key",
    "apiKey": "hwx_live_a1b2c3d4e5f6...",
    "prefix": "hwx_live_a1b2c3...",
    "createdAt": "2026-11-01T00:00:00.000Z"
  }
}
```

### Register Device (`POST /v1/devices/register`)
```bash
curl -X POST http://localhost:3000/v1/devices/register \
  -H "Authorization: Bearer <FIREBASE_ID_TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Pixel 8 Pro Gateway",
    "androidVersion": "14",
    "appVersion": "1.0.0",
    "sims": [
      {
        "subscriptionId": 1,
        "slotIndex": 0,
        "carrierName": "Verizon",
        "phoneNumber": "+1234567890"
      },
      {
        "subscriptionId": 2,
        "slotIndex": 1,
        "carrierName": "T-Mobile",
        "phoneNumber": "+1098765432"
      }
    ]
  }'
```

### Create Bot (`POST /v1/bots`)
```bash
curl -X POST http://localhost:3000/v1/bots \
  -H "Authorization: Bearer <FIREBASE_ID_TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Customer Support Alerts",
    "assignedDeviceId": "dev_e91024cd",
    "assignedSimId": "sim_dev_e91024cd_slot0",
    "simSelectionMode": "auto_switch",
    "fallbackSimIds": ["sim_dev_e91024cd_slot1"],
    "webhookUrl": "https://api.mycrm.com/webhooks/sms"
  }'
```

### Send SMS (`POST /v1/sms/send`)
Can be authenticated with **API Key** or **Firebase Token**:
```bash
curl -X POST http://localhost:3000/v1/sms/send \
  -H "X-API-Key: hwx_live_a1b2c3d4e5f6..." \
  -H "Idempotency-Key: order_tx_90218" \
  -H "Content-Type: application/json" \
  -d '{
    "bot_id": "bot_e91024cd",
    "device_id": "dev_e91024cd",
    "to": "+1234567890",
    "message": "Your order #90218 has been confirmed."
  }'
```

*Response*:
```json
{
  "success": true,
  "data": {
    "jobId": "job_1730000000_a1b2",
    "botId": "bot_e91024cd",
    "deviceId": "dev_e91024cd",
    "simId": "sim_dev_e91024cd_slot0",
    "simSlot": 0,
    "to": "+1234567890",
    "status": "sending",
    "createdAt": "2026-11-01T00:00:00.000Z",
    "dispatchedAt": "2026-11-01T00:00:00.120Z"
  },
  "message": "SMS command dispatched to device"
}
```

---

## 7. Android Device WebSocket Protocol (`/ws/device`)

Android gateway phones maintain a persistent WebSocket connection:

### Connection Handshake
Connect to: `ws://localhost:3000/ws/device?deviceId=dev_...&deviceToken=hwx_dev_...`
Or send initial authentication message:
```json
{
  "type": "AUTH",
  "deviceId": "dev_e91024cd",
  "deviceToken": "hwx_dev_token_..."
}
```

### Server to Device: Send SMS Command
```json
{
  "type": "COMMAND_SEND_SMS",
  "commandId": "cmd_job_1730000000_a1b2",
  "jobId": "job_1730000000_a1b2",
  "to": "+1234567890",
  "message": "Your order #90218 has been confirmed.",
  "simSlot": 0,
  "subscriptionId": 1
}
```

### Device to Server: SMS Result Callback
```json
{
  "type": "SMS_RESULT",
  "jobId": "job_1730000000_a1b2",
  "deviceId": "dev_e91024cd",
  "status": "sent",
  "timestamp": "2026-11-01T00:00:01.000Z"
}
```

### Device to Server: Forward Incoming SMS
```json
{
  "type": "SMS_INCOMING",
  "deviceId": "dev_e91024cd",
  "simSlot": 0,
  "subscriptionId": 1,
  "from": "+1987654321",
  "message": "YES",
  "timestamp": "2026-11-01T00:00:02.000Z"
}
```

---

## 8. Render Deployment Instructions

The backend is fully preconfigured for deployment on [Render](https://render.com) using the included `render.yaml` blueprint.

### Deployment Steps:

1. **Connect Repository on Render**:
   - Log in to your Render dashboard.
   - Click **New** -> **Blueprint**.
   - Select your GitHub/GitLab repository containing this project.
   - Render will detect `render.yaml`.

2. **Configure Environment Variables**:
   In your Render Web Service dashboard under **Environment**:
   - Set `FIREBASE_SERVICE_ACCOUNT` to your complete Firebase Service Account JSON string (single line).
   - Set `PORT` to `3000`.
   - Set `NODE_ENV` to `production`.

3. **Deploy**:
   - Trigger manual deploy or push to `main`.
   - The health check runs against `/health`.

### Operational Note on Free vs Starter Instances:
- **Free Web Services** on Render automatically spin down after 15 minutes of inactivity. When an instance sleeps, active WebSocket connections to Android phones are closed.
- **For production reliability**, use a **Starter plan ($7/mo)** or higher to maintain continuous WebSocket connectivity, uninterrupted SMS reception, and instant outbound command delivery.
- Persistent jobs are stored in Cloud Firestore, guaranteeing that even across server restarts or scaling events, un-dispatched SMS jobs are never lost.

---

## 9. Regulatory Compliance Notice

HWX SMS Gateway is engineered strictly for **legitimate, consent-based, opt-in business messaging** adhering to:
- CTIA Short Code & Long Code Guidelines
- Telephone Consumer Protection Act (TCPA)
- Carrier Application-to-Person (A2P 10DLC) brand and campaign registrations
- Local telecommunication regulations and anti-spam laws
