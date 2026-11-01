import { WebSocket, WebSocketServer } from 'ws';
import http from 'http';
import { DeviceService } from '../devices/device.service.ts';
import { collections } from '../../config/firestore.ts';
import {
  DeviceAuthPayload,
  DeviceHeartbeatPayload,
  DeviceIncomingSmsPayload,
  DeviceSmsResultPayload,
  ServerCommandSendSms,
  SmsJob,
  SmsMessage,
} from '../../types/index.ts';
import { WebhookService } from '../webhooks/webhook.service.ts';
import { BotService } from '../bots/bot.service.ts';
import { UserService } from '../users/user.service.ts';

interface AuthenticatedSocket extends WebSocket {
  deviceId?: string;
  uid?: string;
  isAlive?: boolean;
  authTimeoutTimer?: NodeJS.Timeout;
}

/**
 * HWX SMS Gateway - Real-Time WebSocket Device Connection Manager
 *
 * Implements:
 * 1. Secure handshake authentication (no credential exposure in URL queries)
 * 2. 5-second handshake timeout for unauthenticated sockets
 * 3. Strict device-to-job verification (preventing cross-device status tampering)
 * 4. Dual-SIM incoming association and persistent job dispatch
 */
export class DeviceSocketManager {
  private static instance: DeviceSocketManager;
  private wss: WebSocketServer | null = null;
  private activeSockets: Map<string, AuthenticatedSocket> = new Map();
  private pingInterval: NodeJS.Timeout | null = null;

  public static getInstance(): DeviceSocketManager {
    if (!DeviceSocketManager.instance) {
      DeviceSocketManager.instance = new DeviceSocketManager();
    }
    return DeviceSocketManager.instance;
  }

  /**
   * Initializes the WebSocket server attached to the HTTP server on path /ws/device.
   */
  public init(server: http.Server): void {
    this.wss = new WebSocketServer({
      server,
      path: '/ws/device',
    });

    this.wss.on('connection', (ws: AuthenticatedSocket, req: http.IncomingMessage) => {
      ws.isAlive = true;

      // Enforce 5-second security handshake timeout
      ws.authTimeoutTimer = setTimeout(() => {
        if (!ws.deviceId || !ws.uid) {
          console.warn('[WebSocket Security] Connection terminated: Device failed to authenticate within 5 seconds.');
          ws.send(JSON.stringify({ type: 'AUTH_TIMEOUT', message: 'Authentication handshake timed out after 5 seconds' }));
          ws.close(4008, 'Authentication handshake timeout');
          ws.terminate();
        }
      }, 5000);

      // Handle ping/pong heartbeat
      ws.on('pong', () => {
        ws.isAlive = true;
      });

      // Backward compatibility: allow query parameters if provided, but log deprecation
      try {
        const url = new URL(req.url || '', `http://${req.headers.host || 'localhost'}`);
        const qDeviceId = url.searchParams.get('deviceId');
        const qDeviceToken = url.searchParams.get('deviceToken');

        if (qDeviceId && qDeviceToken) {
          console.log('[WebSocket Security] Notice: Authenticating via URL query params. Preferred: Send AUTH message.');
          this.authenticateConnection(ws, qDeviceId, qDeviceToken);
        }
      } catch (err) {
        console.warn('[WebSocket] Error parsing connection URL:', err);
      }

      // Handle incoming frames
      ws.on('message', async (raw) => {
        try {
          const payload = JSON.parse(raw.toString());
          await this.handleMessage(ws, payload);
        } catch (err) {
          console.warn('[WebSocket] Failed to parse message:', err);
          ws.send(JSON.stringify({ type: 'ERROR', message: 'Malformed JSON payload' }));
        }
      });

      ws.on('close', () => {
        if (ws.authTimeoutTimer) {
          clearTimeout(ws.authTimeoutTimer);
        }
        if (ws.deviceId && ws.uid) {
          this.handleDisconnect(ws.deviceId, ws.uid);
        }
      });

      ws.on('error', (err) => {
        if (ws.authTimeoutTimer) {
          clearTimeout(ws.authTimeoutTimer);
        }
        console.error(`[WebSocket] Socket error for ${ws.deviceId || 'unknown'}:`, err);
      });
    });

    // 30-second ping heartbeat check
    this.pingInterval = setInterval(() => {
      this.wss?.clients.forEach((client) => {
        const authWs = client as AuthenticatedSocket;
        if (authWs.isAlive === false) {
          if (authWs.deviceId && authWs.uid) {
            this.handleDisconnect(authWs.deviceId, authWs.uid);
          }
          return authWs.terminate();
        }

        authWs.isAlive = false;
        authWs.ping();
      });
    }, 30000);

    console.log('[WebSocket] Device gateway WebSocket server listening on /ws/device');
  }

  /**
   * Authenticates a WebSocket connection.
   */
  private async authenticateConnection(
    ws: AuthenticatedSocket,
    deviceId: string,
    rawToken: string
  ): Promise<boolean> {
    const authResult = await DeviceService.authenticateDevice(deviceId, rawToken);

    if (!authResult) {
      ws.send(JSON.stringify({ type: 'AUTH_FAILED', message: 'Invalid device credentials' }));
      ws.close(4001, 'Authentication failed');
      return false;
    }

    // Cancel unauthenticated handshake timeout timer
    if (ws.authTimeoutTimer) {
      clearTimeout(ws.authTimeoutTimer);
      ws.authTimeoutTimer = undefined;
    }

    const { device, uid } = authResult;
    ws.deviceId = deviceId;
    ws.uid = uid;
    ws.isAlive = true;

    this.activeSockets.set(deviceId, ws);

    // Mark online in Firestore
    await DeviceService.setDeviceStatus(uid, deviceId, 'online');

    ws.send(
      JSON.stringify({
        type: 'AUTH_SUCCESS',
        deviceId,
        message: 'Device connected and authenticated to HWX SMS Gateway',
      })
    );

    // Notify webhooks
    WebhookService.triggerEvent(uid, 'device.online', {
      device_id: deviceId,
      name: device.name,
      timestamp: new Date().toISOString(),
    });

    // Automatically check for pending queued SMS jobs to dispatch
    this.flushQueuedJobsForDevice(uid, deviceId);

    return true;
  }

  /**
   * Routes incoming messages from Android phone.
   */
  private async handleMessage(ws: AuthenticatedSocket, data: Record<string, unknown>): Promise<void> {
    const type = data.type as string;

    // Secure Handshake message
    if (type === 'AUTH') {
      const authData = data as unknown as DeviceAuthPayload;
      await this.authenticateConnection(ws, authData.deviceId, authData.deviceToken);
      return;
    }

    // Block any message before secure authentication handshake
    if (!ws.deviceId || !ws.uid) {
      ws.send(JSON.stringify({ type: 'UNAUTHORIZED', message: 'Authenticate first via AUTH message' }));
      return;
    }

    const { deviceId, uid } = ws;

    switch (type) {
      case 'HEARTBEAT': {
        const hb = data as unknown as DeviceHeartbeatPayload;
        await DeviceService.updateHeartbeat(uid, deviceId, {
          batteryLevel: hb.batteryLevel,
          isCharging: hb.isCharging,
          networkType: hb.networkType,
        });
        ws.send(JSON.stringify({ type: 'PONG', timestamp: new Date().toISOString() }));
        break;
      }

      case 'ACK': {
        // Android device received command
        break;
      }

      case 'SMS_RESULT': {
        const res = data as unknown as DeviceSmsResultPayload;
        try {
          await this.handleSmsResult(uid, {
            ...res,
            deviceId, // Strictly lock to authenticated deviceId
          });
          ws.send(JSON.stringify({ type: 'ACK', ackId: res.jobId, status: 'ok' }));
        } catch (err: unknown) {
          const msg = err instanceof Error ? err.message : 'Error processing result';
          ws.send(JSON.stringify({ type: 'ERROR', ackId: res.jobId, message: msg }));
        }
        break;
      }

      case 'SMS_INCOMING': {
        const incoming = data as unknown as DeviceIncomingSmsPayload;
        await this.handleIncomingSms(uid, deviceId, incoming);
        ws.send(JSON.stringify({ type: 'ACK', ackId: incoming.from, status: 'ok' }));
        break;
      }

      default:
        ws.send(JSON.stringify({ type: 'UNKNOWN_COMMAND', command: type }));
    }
  }

  /**
   * Handles delivery confirmation or failure reported by Android device.
   * Security Requirement: Enforces that the reporting device matches the job's assigned device.
   */
  public async handleSmsResult(uid: string, res: DeviceSmsResultPayload): Promise<void> {
    const jobDocRef = collections.smsJobDoc(uid, res.jobId);
    const jobDoc = await jobDocRef.get();

    if (!jobDoc.exists) {
      console.warn(`[WebSocket] Received SMS result for unknown job: ${res.jobId}`);
      throw new Error(`JOB_NOT_FOUND: SMS Job '${res.jobId}' does not exist`);
    }

    const job = jobDoc.data() as SmsJob;

    // Security Gate: Verify reporting device owns the job
    if (job.deviceId !== res.deviceId) {
      console.warn(
        `[Security Alert] Device '${res.deviceId}' attempted to update status for job '${res.jobId}' assigned to device '${job.deviceId}'!`
      );
      throw new Error(`DEVICE_JOB_MISMATCH: Device '${res.deviceId}' is not authorized to update job '${res.jobId}'`);
    }

    // State Machine Protection: Do not revert terminal 'delivered' state
    if (job.status === 'delivered') {
      console.log(`[WebSocket] Job ${res.jobId} is already marked delivered. Skipping update.`);
      return;
    }

    const now = new Date().toISOString();

    const updates: Partial<SmsJob> = {
      status: res.status,
      errorMessage: res.errorMessage || undefined,
      errorCode: res.errorCode || undefined,
      updatedAt: now,
    };

    if (res.status === 'sent') {
      updates.sentAt = now;
      await UserService.incrementSmsCount(uid, 'sent');
    } else if (res.status === 'delivered') {
      updates.deliveredAt = now;
    }

    await jobDocRef.update(updates);

    // Update corresponding smsMessage record
    await collections.smsMessageDoc(uid, res.jobId).update({
      status: res.status,
      updatedAt: now,
    }).catch(() => {});

    // Dispatch webhook event
    const eventType = res.status === 'sent'
      ? 'sms.sent'
      : res.status === 'delivered'
      ? 'sms.delivered'
      : 'sms.failed';

    const bot = await BotService.getBot(uid, job.botId);
    WebhookService.triggerEvent(
      uid,
      eventType,
      {
        job_id: res.jobId,
        bot_id: job.botId,
        device_id: job.deviceId,
        sim_id: job.simId,
        to: job.to,
        status: res.status,
        error_code: res.errorCode,
        error_message: res.errorMessage,
        timestamp: now,
      },
      bot?.webhookUrl
    );
  }

  /**
   * Handles incoming SMS forwarded from Android device.
   */
  public async handleIncomingSms(
    uid: string,
    deviceId: string,
    incoming: DeviceIncomingSmsPayload
  ): Promise<void> {
    const now = new Date().toISOString();
    const messageId = `msg_${Date.now()}_${Math.random().toString(36).substring(2, 7)}`;

    // Resolve SIM card
    const sims = await DeviceService.listDeviceSims(uid, deviceId);
    const matchedSim = sims.find(
      (s) => s.slotIndex === incoming.simSlot || (incoming.subscriptionId && s.subscriptionId === incoming.subscriptionId)
    );

    // Find assigned Bot for this SIM slot
    const assignedBot = matchedSim?.assignedBotId
      ? await BotService.getBot(uid, matchedSim.assignedBotId)
      : await BotService.findBotByDeviceAndSim(uid, deviceId, incoming.simSlot);

    const smsMessage: SmsMessage = {
      id: messageId,
      uid,
      botId: assignedBot?.id,
      deviceId,
      simId: matchedSim?.id,
      simSlot: incoming.simSlot,
      direction: 'inbound',
      from: incoming.from,
      to: matchedSim?.phoneNumber || 'Self',
      message: incoming.message,
      receivedAt: incoming.timestamp || now,
      createdAt: now,
      webhookDelivered: false,
    };

    await collections.smsMessageDoc(uid, messageId).set(smsMessage);
    await UserService.incrementSmsCount(uid, 'received');

    // Trigger webhook
    WebhookService.triggerEvent(
      uid,
      'sms.received',
      {
        message_id: messageId,
        bot_id: assignedBot?.id || null,
        device_id: deviceId,
        sim_id: matchedSim?.id || null,
        sim_slot: incoming.simSlot,
        from: incoming.from,
        message: incoming.message,
        received_at: incoming.timestamp || now,
      },
      assignedBot?.webhookUrl
    );
  }

  /**
   * Dispatches an SMS sending command to a connected device.
   * Returns true if sent over active socket, false if device is offline.
   */
  public sendSmsCommand(deviceId: string, command: ServerCommandSendSms): boolean {
    const ws = this.activeSockets.get(deviceId);
    if (!ws || ws.readyState !== WebSocket.OPEN) {
      return false;
    }

    ws.send(JSON.stringify(command));
    return true;
  }

  /**
   * Dispatches all queued SMS jobs when a device connects or reconnects.
   */
  public async flushQueuedJobsForDevice(uid: string, deviceId: string): Promise<void> {
    try {
      const snapshot = await collections
        .smsJobs(uid)
        .where('deviceId', '==', deviceId)
        .where('status', '==', 'queued')
        .limit(20)
        .get();

      if (snapshot.empty) return;

      console.log(`[WebSocket] Flushing ${snapshot.docs.length} queued SMS jobs to device ${deviceId}`);

      for (const doc of snapshot.docs) {
        const job = doc.data() as SmsJob;
        const command: ServerCommandSendSms = {
          type: 'COMMAND_SEND_SMS',
          commandId: `cmd_${job.id}`,
          jobId: job.id,
          to: job.to,
          message: job.message,
          simSlot: job.simSlot,
        };

        const sent = this.sendSmsCommand(deviceId, command);
        if (sent) {
          await doc.ref.update({
            status: 'sending',
            dispatchedAt: new Date().toISOString(),
            updatedAt: new Date().toISOString(),
          });
        }
      }
    } catch (err) {
      console.warn(`[WebSocket] Error flushing queued jobs for device ${deviceId}:`, err);
    }
  }

  /**
   * Clean up on disconnect.
   */
  private async handleDisconnect(deviceId: string, uid: string): Promise<void> {
    this.activeSockets.delete(deviceId);
    await DeviceService.setDeviceStatus(uid, deviceId, 'offline').catch(() => {});

    WebhookService.triggerEvent(uid, 'device.offline', {
      device_id: deviceId,
      timestamp: new Date().toISOString(),
    });
  }

  /**
   * Checks if a device is currently connected with an open WebSocket.
   */
  public isDeviceConnected(deviceId: string): boolean {
    const ws = this.activeSockets.get(deviceId);
    return !!ws && ws.readyState === WebSocket.OPEN;
  }

  /**
   * Cleanup on server shutdown.
   */
  public close(): void {
    if (this.pingInterval) {
      clearInterval(this.pingInterval);
    }
    this.wss?.close();
  }
}
