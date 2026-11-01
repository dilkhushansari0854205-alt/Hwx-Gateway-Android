import crypto from 'crypto';
import { QueryDocumentSnapshot } from 'firebase-admin/firestore';
import { collections } from '../../config/firestore.ts';
import { WebhookConfig, WebhookDelivery, WebhookEvent } from '../../types/index.ts';
import {
  generateHmacSignature,
  validateWebhookUrl,
} from '../../services/security.service.ts';
import { config } from '../../config/env.ts';

/**
 * HWX SMS Gateway - Webhook Dispatch and Delivery Service
 *
 * Implements:
 * 1. HMAC-SHA256 signatures (sent in X-HWX-Signature)
 * 2. Multi-hop SSRF protection with redirect validation
 * 3. Exponential backoff retries with delivery tracking
 * 4. Request timeout enforcement
 * 5. Event delivery logs in Firestore
 */
export class WebhookService {
  /**
   * Registers a new Webhook configuration for the user.
   */
  static async createWebhook(
    uid: string,
    data: {
      url: string;
      events: WebhookEvent[];
      secret?: string;
    }
  ): Promise<WebhookConfig> {
    const urlCheck = await validateWebhookUrl(data.url);
    if (!urlCheck.valid) {
      throw new Error(`SSRF Validation Failed: ${urlCheck.reason}`);
    }

    const webhookId = `wh_${crypto.randomBytes(8).toString('hex')}`;
    const secret = data.secret || `whsec_${crypto.randomBytes(24).toString('hex')}`;
    const now = new Date().toISOString();

    const configRecord: WebhookConfig = {
      id: webhookId,
      uid,
      url: data.url,
      secret,
      events: data.events.length > 0 ? data.events : ['sms.received'],
      isActive: true,
      createdAt: now,
      updatedAt: now,
    };

    await collections.webhookDoc(uid, webhookId).set(configRecord);
    return configRecord;
  }

  /**
   * Lists all webhooks configured by a user.
   */
  static async listWebhooks(uid: string): Promise<WebhookConfig[]> {
    const snapshot = await collections.webhooks(uid).orderBy('createdAt', 'desc').get();
    return snapshot.docs.map((doc: QueryDocumentSnapshot) => doc.data() as WebhookConfig);
  }

  /**
   * Retrieves a specific webhook by ID.
   */
  static async getWebhook(uid: string, webhookId: string): Promise<WebhookConfig | null> {
    const doc = await collections.webhookDoc(uid, webhookId).get();
    if (!doc.exists) return null;
    return doc.data() as WebhookConfig;
  }

  /**
   * Updates an existing webhook configuration.
   */
  static async updateWebhook(
    uid: string,
    webhookId: string,
    updates: Partial<{
      url: string;
      events: WebhookEvent[];
      secret: string;
      isActive: boolean;
    }>
  ): Promise<WebhookConfig> {
    const existing = await this.getWebhook(uid, webhookId);
    if (!existing) {
      throw new Error(`Webhook '${webhookId}' not found`);
    }

    if (updates.url) {
      const urlCheck = await validateWebhookUrl(updates.url);
      if (!urlCheck.valid) {
        throw new Error(`SSRF Validation Failed: ${urlCheck.reason}`);
      }
    }

    const now = new Date().toISOString();
    const updatedData: Partial<WebhookConfig> = {
      ...updates,
      updatedAt: now,
    };

    await collections.webhookDoc(uid, webhookId).update(updatedData);
    return { ...existing, ...updatedData };
  }

  /**
   * Deletes a webhook.
   */
  static async deleteWebhook(uid: string, webhookId: string): Promise<boolean> {
    const existing = await this.getWebhook(uid, webhookId);
    if (!existing) return false;

    await collections.webhookDoc(uid, webhookId).delete();
    return true;
  }

  /**
   * Dispatches a webhook payload with HMAC-SHA256, timeout, retry logic,
   * and strict SSRF redirect re-validation.
   */
  static async sendWebhookRequest(
    targetUrl: string,
    secret: string,
    payload: Record<string, unknown>,
    maxRetries = 2
  ): Promise<{ success: boolean; statusCode?: number; responseBody?: string; error?: string }> {
    const rawBody = JSON.stringify(payload);
    const signature = generateHmacSignature(rawBody, secret);
    const timeoutMs = config.WEBHOOK_TIMEOUT_MS || 10000;

    let attempt = 0;
    let lastError = '';
    let lastStatusCode = 0;

    while (attempt <= maxRetries) {
      attempt++;
      let currentUrl = targetUrl;
      let redirectHops = 0;
      const maxRedirectHops = 3;
      let requestFinished = false;

      while (redirectHops < maxRedirectHops && !requestFinished) {
        // SSRF Check on initial URL and on EVERY redirect target URL
        const validation = await validateWebhookUrl(currentUrl);
        if (!validation.valid) {
          return { success: false, error: `SSRF blocked on URL '${currentUrl}': ${validation.reason}` };
        }

        const controller = new AbortController();
        const timeoutId = setTimeout(() => controller.abort(), timeoutMs);

        try {
          const response = await fetch(currentUrl, {
            method: 'POST',
            redirect: 'manual', // Never follow redirects blindly without re-checking SSRF!
            headers: {
              'Content-Type': 'application/json',
              'User-Agent': 'HWX-SMS-Gateway-Webhook/1.0',
              'X-HWX-Signature': signature,
              'X-HWX-Event': String(payload.event || ''),
              'X-HWX-Event-ID': String(payload.event_id || ''),
              'X-HWX-Delivery-Attempt': String(attempt),
            },
            body: rawBody,
            signal: controller.signal,
          });

          clearTimeout(timeoutId);
          lastStatusCode = response.status;

          // Check for redirect (301, 302, 307, 308)
          if ([301, 302, 307, 308].includes(response.status)) {
            const locationHeader = response.headers.get('location');
            if (!locationHeader) {
              lastError = `Redirect status ${response.status} returned without Location header`;
              break;
            }

            const resolvedRedirectUrl = new URL(locationHeader, currentUrl).toString();
            redirectHops++;
            console.log(`[Webhook] Validating redirect hop #${redirectHops} to: ${resolvedRedirectUrl}`);
            currentUrl = resolvedRedirectUrl;
            continue; // Loop and re-validate before fetching
          }

          const textResponse = await response.text();

          if (response.ok) {
            return {
              success: true,
              statusCode: response.status,
              responseBody: textResponse.substring(0, 500),
            };
          }

          lastError = `HTTP ${response.status}: ${textResponse.substring(0, 200)}`;
          requestFinished = true;
        } catch (err: unknown) {
          clearTimeout(timeoutId);
          lastError = err instanceof Error ? err.message : String(err);
          requestFinished = true;
        }
      }

      // Exponential backoff wait before retry
      if (attempt <= maxRetries) {
        const backoffMs = attempt * 1500;
        await new Promise((resolve) => setTimeout(resolve, backoffMs));
      }
    }

    return {
      success: false,
      statusCode: lastStatusCode,
      error: lastError,
    };
  }

  /**
   * Broadcasts an event to all matching user webhooks and records delivery logs.
   */
  static async triggerEvent<T extends Record<string, unknown>>(
    uid: string,
    event: WebhookEvent,
    data: T,
    directWebhookUrl?: string
  ): Promise<void> {
    const eventId = `evt_${crypto.randomBytes(12).toString('hex')}`;
    const payload = {
      event,
      event_id: eventId,
      ...data,
      received_at: data.received_at || new Date().toISOString(),
    };

    const deliveryPromises: Promise<void>[] = [];

    // 1. Direct Bot Webhook if present
    if (directWebhookUrl) {
      deliveryPromises.push(
        (async () => {
          const deliveryId = `del_${crypto.randomBytes(8).toString('hex')}`;
          const result = await this.sendWebhookRequest(directWebhookUrl, 'hwx_bot_secret', payload);

          const deliveryLog: WebhookDelivery = {
            id: deliveryId,
            uid,
            webhookId: 'bot_direct',
            event,
            payload,
            statusCode: result.statusCode,
            responseBody: result.responseBody,
            errorMessage: result.error,
            attempt: 1,
            success: result.success,
            deliveredAt: new Date().toISOString(),
          };

          await collections.webhookDeliveryDoc(uid, deliveryId).set(deliveryLog).catch(() => {});
        })()
      );
    }

    // 2. User Registered Webhooks for this event
    const registeredWebhooks = await this.listWebhooks(uid);
    const matching = registeredWebhooks.filter((w) => w.isActive && w.events.includes(event));

    for (const hook of matching) {
      deliveryPromises.push(
        (async () => {
          const deliveryId = `del_${crypto.randomBytes(8).toString('hex')}`;
          const result = await this.sendWebhookRequest(hook.url, hook.secret, payload);

          const deliveryLog: WebhookDelivery = {
            id: deliveryId,
            uid,
            webhookId: hook.id,
            event,
            payload,
            statusCode: result.statusCode,
            responseBody: result.responseBody,
            errorMessage: result.error,
            attempt: 1,
            success: result.success,
            deliveredAt: new Date().toISOString(),
          };

          await collections.webhookDeliveryDoc(uid, deliveryId).set(deliveryLog).catch(() => {});
        })()
      );
    }

    // Execute in background
    Promise.allSettled(deliveryPromises).catch(() => {});
  }
}
