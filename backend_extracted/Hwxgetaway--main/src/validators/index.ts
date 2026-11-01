import { z } from 'zod';

/**
 * HWX SMS Gateway - Request Validation Schemas (Zod)
 */

export const registerDeviceSchema = z.object({
  name: z.string().min(1, 'Device name is required').max(100),
  androidVersion: z.string().min(1).max(50),
  appVersion: z.string().min(1).max(50),
  sims: z
    .array(
      z.object({
        subscriptionId: z.number().int(),
        slotIndex: z.number().int().min(0).max(1),
        carrierName: z.string().min(1).max(100),
        phoneNumber: z.string().optional(),
        displayName: z.string().optional(),
      })
    )
    .optional(),
});

export const heartbeatSchema = z.object({
  batteryLevel: z.number().min(0).max(100).optional(),
  isCharging: z.boolean().optional(),
  networkType: z.string().max(50).optional(),
});

export const createApiKeySchema = z.object({
  name: z.string().min(1, 'Key name is required').max(100),
  expiresAt: z.string().datetime({ message: 'expiresAt must be a valid ISO-8601 string' }).optional(),
});

export const createBotSchema = z.object({
  name: z.string().min(1, 'Bot name is required').max(100),
  description: z.string().max(500).optional(),
  assignedDeviceId: z.string().min(1, 'Assigned device ID is required'),
  assignedSimId: z.string().min(1, 'Assigned SIM ID is required'),
  simSelectionMode: z.enum(['manual', 'auto_switch', 'bot_fixed']).default('bot_fixed'),
  fallbackSimIds: z.array(z.string()).optional(),
  webhookUrl: z.string().url('webhookUrl must be a valid URL').optional(),
});

export const updateBotSchema = z.object({
  name: z.string().min(1).max(100).optional(),
  description: z.string().max(500).optional(),
  assignedDeviceId: z.string().min(1).optional(),
  assignedSimId: z.string().min(1).optional(),
  simSelectionMode: z.enum(['manual', 'auto_switch', 'bot_fixed']).optional(),
  fallbackSimIds: z.array(z.string()).optional(),
  webhookUrl: z.string().url().optional(),
  status: z.enum(['active', 'inactive']).optional(),
});

export const sendSmsSchema = z.object({
  bot_id: z.string().min(1, 'bot_id is required'),
  device_id: z.string().min(1, 'device_id is required'),
  to: z
    .string()
    .min(3, 'Recipient phone number must have at least 3 digits')
    .max(30)
    .regex(/^\+?[0-9\s\-()]+$/, 'Invalid phone number format'),
  message: z.string().min(1, 'Message cannot be empty').max(1600, 'Message cannot exceed 1600 characters'),
  sim_id: z.string().optional(),
  reply_to_message_id: z.string().optional(),
  idempotency_key: z.string().max(100).optional(),
});

export const updateSimSchema = z.object({
  isVerified: z.boolean().optional(),
  isAuthorizedFallback: z.boolean().optional(),
  displayName: z.string().max(100).optional(),
  phoneNumber: z.string().max(30).optional(),
});

export const deviceIncomingSmsSchema = z.object({
  simSlot: z.number().int().min(0).max(1),
  subscriptionId: z.number().int().optional(),
  from: z.string().min(1, 'Sender number is required'),
  message: z.string().min(1, 'Message is required'),
  timestamp: z.string().optional(),
});

export const deviceSmsResultSchema = z.object({
  jobId: z.string().min(1, 'jobId is required'),
  status: z.enum(['sent', 'delivered', 'failed']),
  errorCode: z.string().optional(),
  errorMessage: z.string().optional(),
  timestamp: z.string().optional(),
});

export const createWebhookSchema = z.object({
  url: z.string().url('Valid URL is required'),
  events: z.array(
    z.enum(['sms.received', 'sms.sent', 'sms.failed', 'sms.delivered', 'device.online', 'device.offline'])
  ).min(1, 'At least one event must be selected'),
  secret: z.string().min(8, 'Webhook secret must be at least 8 characters').optional(),
});

export const updateWebhookSchema = z.object({
  url: z.string().url().optional(),
  events: z
    .array(
      z.enum(['sms.received', 'sms.sent', 'sms.failed', 'sms.delivered', 'device.online', 'device.offline'])
    )
    .optional(),
  secret: z.string().min(8).optional(),
  isActive: z.boolean().optional(),
});

export const testWebhookSchema = z.object({
  url: z.string().url('Valid URL is required'),
  secret: z.string().optional(),
});


export const createDeviceEnrollmentSchema = registerDeviceSchema;


export const syncDeviceSimsSchema = z.object({
  sims: z.array(
    z.object({
      subscriptionId: z.number().int(),
      slotIndex: z.number().int().min(0).max(1),
      carrierName: z.string().min(1).max(100),
      phoneNumber: z.string().max(30).optional(),
      displayName: z.string().max(100).optional(),
    })
  ).max(2),
});
