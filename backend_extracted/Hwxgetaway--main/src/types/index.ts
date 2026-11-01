/**
 * HWX SMS Gateway - Domain Types and Interfaces
 */

export type UserStatus = 'active' | 'suspended' | 'pending';

export interface UserStats {
  smsSentCount: number;
  smsReceivedCount: number;
  apiRequestsCount: number;
  activeDevicesCount: number;
  lastActiveAt?: string;
}

export interface UserProfile {
  uid: string;
  email: string;
  displayName?: string;
  photoURL?: string;
  emailVerified: boolean;
  status: UserStatus;
  createdAt: string;
  updatedAt: string;
  stats: UserStats;
}

export interface ApiKeyRecord {
  id: string;
  uid: string;
  name: string;
  keyPrefix: string; // e.g. "hwx_live_a1b2..."
  keyHash: string;   // SHA-256 hex string
  createdAt: string;
  expiresAt?: string;
  lastUsedAt?: string;
  usageCount: number;
  status: 'active' | 'revoked';
}

export interface CreateApiKeyResponse {
  id: string;
  name: string;
  apiKey: string; // Plaintext key, shown ONCE upon creation
  prefix: string;
  createdAt: string;
  expiresAt?: string;
}

export type DeviceStatus = 'online' | 'offline' | 'disabled';

export interface SimCard {
  id: string;
  subscriptionId: number;
  slotIndex: number; // 0 for SIM 1, 1 for SIM 2
  carrierName: string;
  displayName?: string;
  phoneNumber?: string;
  isVerified: boolean;
  assignedBotId?: string;
  isAuthorizedFallback: boolean;
  lastVerifiedAt?: string;
  createdAt: string;
  updatedAt: string;
}

export interface Device {
  id: string;
  uid: string;
  name: string;
  androidVersion: string;
  appVersion: string;
  status: DeviceStatus;
  deviceTokenHash: string; // Hashed authentication secret for the phone
  batteryLevel?: number;
  isCharging?: boolean;
  networkType?: string;
  ipAddress?: string;
  lastSeen: string;
  registeredAt: string;
  updatedAt: string;
  sims: SimCard[];
}

export type SimSelectionMode = 'manual' | 'auto_switch' | 'bot_fixed';

export interface Bot {
  id: string;
  uid: string;
  name: string;
  description?: string;
  assignedDeviceId: string;
  assignedSimId: string;
  simSelectionMode: SimSelectionMode;
  fallbackSimIds?: string[]; // Only used when simSelectionMode is auto_switch
  webhookUrl?: string;
  status: 'active' | 'inactive';
  createdAt: string;
  updatedAt: string;
}

export type SmsStatus =
  | 'queued'
  | 'accepted'
  | 'sending'
  | 'sent'
  | 'failed'
  | 'delivered';

export type SmsDirection = 'outbound' | 'inbound';

export interface SmsJob {
  id: string;
  uid: string;
  botId: string;
  deviceId: string;
  simId: string;
  simSlot: number;
  to: string;
  from?: string;
  message: string;
  status: SmsStatus;
  idempotencyKey?: string;
  replyToMessageId?: string;
  errorMessage?: string;
  errorCode?: string;
  dispatchedAt?: string;
  sentAt?: string;
  deliveredAt?: string;
  createdAt: string;
  updatedAt: string;
}

export interface SmsMessage {
  id: string;
  uid: string;
  botId?: string;
  deviceId: string;
  simId?: string;
  simSlot?: number;
  direction: SmsDirection;
  replyToMessageId?: string;
  from: string;
  to: string;
  message: string;
  receivedAt?: string;
  createdAt: string;
  webhookDelivered?: boolean;
}

export type WebhookEvent =
  | 'sms.received'
  | 'sms.sent'
  | 'sms.failed'
  | 'sms.delivered'
  | 'device.online'
  | 'device.offline';

export interface WebhookConfig {
  id: string;
  uid: string;
  url: string;
  secret: string; // Secret used to sign payloads via HMAC-SHA256
  events: WebhookEvent[];
  isActive: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface WebhookDelivery {
  id: string;
  uid: string;
  webhookId: string;
  event: WebhookEvent;
  payload: Record<string, unknown>;
  statusCode?: number;
  responseBody?: string;
  errorMessage?: string;
  attempt: number;
  success: boolean;
  deliveredAt: string;
}

export interface WebhookPayload<T = unknown> {
  event: WebhookEvent;
  event_id: string;
  timestamp: string;
  data: T;
}

// WebSocket message protocols
export interface DeviceAuthPayload {
  type: 'AUTH';
  deviceId: string;
  deviceToken: string;
}

export interface DeviceHeartbeatPayload {
  type: 'HEARTBEAT';
  deviceId: string;
  batteryLevel?: number;
  isCharging?: boolean;
  networkType?: string;
}

export interface DeviceSmsResultPayload {
  type: 'SMS_RESULT';
  jobId: string;
  deviceId: string;
  status: 'sent' | 'failed' | 'delivered';
  errorCode?: string;
  errorMessage?: string;
  timestamp: string;
}

export interface DeviceIncomingSmsPayload {
  type: 'SMS_INCOMING';
  deviceId: string;
  simSlot: number;
  subscriptionId?: number;
  from: string;
  message: string;
  timestamp: string;
}

export interface ServerCommandSendSms {
  type: 'COMMAND_SEND_SMS';
  commandId: string;
  jobId: string;
  to: string;
  message: string;
  simSlot: number;
  subscriptionId?: number;
}

export interface ServerCommandAck {
  type: 'ACK';
  ackId: string;
  status: 'ok' | 'error';
  message?: string;
}
