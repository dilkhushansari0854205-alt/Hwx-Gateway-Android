# HWX Gateway – Trusted Multi-Device Enrollment

## Policy

- The first phone registered to an account becomes its initial trusted device.
- Every later phone must sign in with the same Firebase account and request approval.
- A trusted phone can approve or reject a pending request.
- A new phone receives its Device ID and Device Token only after approval.
- Device Tokens are stored hashed on the backend and returned to the new phone only when the approved request is consumed.
- Requests expire after 10 minutes.
- There is no configured per-account device-count cap. This does not mean infinite infrastructure capacity; service-level abuse/rate limits and provider quotas still apply.

## API

All endpoints are under `/v1/devices`.

| Method | Path | Authentication | Purpose |
|---|---|---|---|
| POST | `/register` | Firebase ID token | Bootstrap the first trusted phone only |
| POST | `/enrollment-requests` | Firebase ID token | Create a 10-minute pending request |
| GET | `/enrollment-requests/pending` | Existing Device ID + Device Token | List pending requests for the account |
| POST | `/enrollment-requests/:requestId/approve` | Existing Device ID + Device Token | Approve a request |
| POST | `/enrollment-requests/:requestId/reject` | Existing Device ID + Device Token | Reject a request |
| GET | `/enrollment-requests/:requestId` | Firebase ID token | Poll request status; approved request is consumed once and returns new device credentials |
| POST | `/:deviceId/sims/sync` | Matching Device ID + Device Token | Sync SIM metadata only after the phone has been approved |

## Android behavior

After Firebase login, `DeviceSetupGateScreen` checks local device credentials. If this phone is already registered, it proceeds to the dashboard. If the account has no devices, it registers this phone as the first trusted device. If the account already has devices, it creates a pending request and opens `DeviceApprovalScreen`.

The Devices screen polls pending requests and presents Approve/Reject controls to a trusted phone. The new phone shares SIM metadata only after approval; SIM authorization and fallback remain disabled until the user explicitly configures them.

## Important deployment note

These changes are source-code changes only. They have not been deployed to Render or Firebase. Before release, install backend dependencies, run `npm run lint` and `npm test`, verify Firestore rules/indexes and server environment, deploy the backend, then build and test the Android APK on at least two physical phones.

## Operational recovery

If the only trusted phone is lost or removed, the user cannot approve a new phone using this flow. A separately authenticated account-recovery process is required before production launch; do not add an unauthenticated bypass to `/register`.
