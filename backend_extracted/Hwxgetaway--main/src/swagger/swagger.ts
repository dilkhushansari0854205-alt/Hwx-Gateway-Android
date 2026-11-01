/**
 * HWX SMS Gateway - OpenAPI 3.0 / Swagger Specification
 */
export const swaggerDocument = {
  openapi: '3.0.3',
  info: {
    title: 'HWX SMS Gateway API',
    version: '1.0.0',
    description:
      'Production-oriented multi-user Android SMS Gateway backend server. ' +
      'Coordinates dual-SIM Android devices, Bot orchestration, real-time WebSocket communication, and secure webhooks.',
    contact: {
      name: 'HWX Gateway Engineering',
      email: 'support@hwxgetway.com',
    },
  },
  servers: [
    {
      url: '/v1',
      description: 'API v1 Root',
    },
    {
      url: '/',
      description: 'Server Root',
    },
  ],
  components: {
    securitySchemes: {
      FirebaseAuth: {
        type: 'http',
        scheme: 'bearer',
        bearerFormat: 'JWT',
        description: 'Firebase ID Token obtained from Google Login or Firebase Auth on Android/Web',
      },
      ApiKeyAuth: {
        type: 'apiKey',
        in: 'header',
        name: 'X-API-Key',
        description: 'Server-to-server API key (e.g. hwx_live_...)',
      },
      DeviceTokenAuth: {
        type: 'apiKey',
        in: 'header',
        name: 'X-Device-Token',
        description: 'Secret token issued to registered Android device',
      },
      DeviceIdHeader: {
        type: 'apiKey',
        in: 'header',
        name: 'X-Device-Id',
        description: 'Registered Android device ID (e.g. dev_...)',
      },
    },
    schemas: {
      ErrorResponse: {
        type: 'object',
        properties: {
          success: { type: 'boolean', example: false },
          error: {
            type: 'object',
            properties: {
              code: { type: 'string', example: 'VALIDATION_ERROR' },
              message: { type: 'string', example: 'Invalid request payload' },
              details: { type: 'array', items: { type: 'object' } },
            },
          },
        },
      },
      SendSmsRequest: {
        type: 'object',
        required: ['bot_id', 'device_id', 'to', 'message'],
        properties: {
          bot_id: { type: 'string', example: 'bot_8f43a91b' },
          device_id: { type: 'string', example: 'dev_e91024cd' },
          to: { type: 'string', example: '+1234567890' },
          message: { type: 'string', example: 'Your verification code is 492041' },
          sim_id: { type: 'string', example: 'sim_dev_e91024cd_slot0' },
          idempotency_key: { type: 'string', example: 'order_tx_98124' },
        },
      },
      RegisterDeviceRequest: {
        type: 'object',
        required: ['name', 'androidVersion', 'appVersion'],
        properties: {
          name: { type: 'string', example: 'Pixel 8 Pro - Gateway 1' },
          androidVersion: { type: 'string', example: '14.0' },
          appVersion: { type: 'string', example: '2.1.0' },
          sims: {
            type: 'array',
            items: {
              type: 'object',
              properties: {
                subscriptionId: { type: 'integer', example: 1 },
                slotIndex: { type: 'integer', example: 0 },
                carrierName: { type: 'string', example: 'Verizon' },
                phoneNumber: { type: 'string', example: '+1234567890' },
              },
            },
          },
        },
      },
      CreateBotRequest: {
        type: 'object',
        required: ['name', 'assignedDeviceId', 'assignedSimId'],
        properties: {
          name: { type: 'string', example: 'Customer Support Bot' },
          description: { type: 'string', example: 'Dispatches support alerts' },
          assignedDeviceId: { type: 'string', example: 'dev_e91024cd' },
          assignedSimId: { type: 'string', example: 'sim_dev_e91024cd_slot0' },
          simSelectionMode: {
            type: 'string',
            enum: ['manual', 'auto_switch', 'bot_fixed'],
            example: 'bot_fixed',
          },
          fallbackSimIds: {
            type: 'array',
            items: { type: 'string' },
            example: ['sim_dev_e91024cd_slot1'],
          },
          webhookUrl: { type: 'string', example: 'https://api.mycrm.com/webhooks/sms' },
        },
      },
    },
  },
  paths: {
    '/health': {
      get: {
        summary: 'Health Check',
        description: 'Returns server health, uptime, memory, and database status',
        responses: {
          200: {
            description: 'Server is healthy',
          },
        },
      },
    },
    '/auth/me': {
      get: {
        summary: 'Get Authenticated User Profile',
        security: [{ FirebaseAuth: [] }],
        responses: {
          200: { description: 'User profile with statistics' },
          401: { description: 'Unauthorized' },
        },
      },
    },
    '/api-keys': {
      get: {
        summary: 'List API Keys',
        security: [{ FirebaseAuth: [] }],
        responses: {
          200: { description: 'List of user API keys (hashed keys hidden)' },
        },
      },
      post: {
        summary: 'Create API Key',
        security: [{ FirebaseAuth: [] }],
        requestBody: {
          required: true,
          content: {
            'application/json': {
              schema: {
                type: 'object',
                required: ['name'],
                properties: {
                  name: { type: 'string', example: 'Production Backend Key' },
                  expiresAt: { type: 'string', format: 'date-time' },
                },
              },
            },
          },
        },
        responses: {
          201: { description: 'API key created. Raw key shown once.' },
        },
      },
    },
    '/api-keys/{keyId}': {
      delete: {
        summary: 'Revoke API Key',
        security: [{ FirebaseAuth: [] }],
        parameters: [{ in: 'path', name: 'keyId', required: true, schema: { type: 'string' } }],
        responses: {
          200: { description: 'Key revoked' },
        },
      },
    },
    '/devices/register': {
      post: {
        summary: 'Register Android Device',
        security: [{ FirebaseAuth: [] }],
        requestBody: {
          required: true,
          content: {
            'application/json': {
              schema: { $ref: '#/components/schemas/RegisterDeviceRequest' },
            },
          },
        },
        responses: {
          201: { description: 'Device registered. Token returned once.' },
        },
      },
    },
    '/devices/enrollment-requests': {
      post: {
        summary: 'Request approval for a new phone',
        description: 'Creates a 10-minute pending enrollment request. The new phone receives no Device Token until an existing trusted phone approves it.',
        security: [{ FirebaseAuth: [] }],
        requestBody: {
          required: true,
          content: { 'application/json': { schema: { $ref: '#/components/schemas/RegisterDeviceRequest' } } },
        },
        responses: { 202: { description: 'Approval request created' }, 409: { description: 'No trusted phone exists yet' } },
      },
    },
    '/devices/enrollment-requests/pending': {
      get: {
        summary: 'List pending phone approval requests',
        security: [{ DeviceIdHeader: [], DeviceTokenAuth: [] }],
        responses: { 200: { description: 'Pending requests for this account' } },
      },
    },
    '/devices/enrollment-requests/{requestId}': {
      get: {
        summary: 'Poll new phone approval status',
        security: [{ FirebaseAuth: [] }],
        parameters: [{ in: 'path', name: 'requestId', required: true, schema: { type: 'string' } }],
        responses: { 200: { description: 'Status; a newly approved request returns the Device Token once' } },
      },
    },
    '/devices/enrollment-requests/{requestId}/approve': {
      post: {
        summary: 'Approve new phone',
        security: [{ DeviceIdHeader: [], DeviceTokenAuth: [] }],
        parameters: [{ in: 'path', name: 'requestId', required: true, schema: { type: 'string' } }],
        responses: { 200: { description: 'New phone approved' }, 409: { description: 'Request expired or already handled' } },
      },
    },
    '/devices/enrollment-requests/{requestId}/reject': {
      post: {
        summary: 'Reject new phone',
        security: [{ DeviceIdHeader: [], DeviceTokenAuth: [] }],
        parameters: [{ in: 'path', name: 'requestId', required: true, schema: { type: 'string' } }],
        responses: { 200: { description: 'New phone rejected' }, 409: { description: 'Request expired or already handled' } },
      },
    },
    '/devices': {
      get: {
        summary: 'List Registered Devices',
        security: [{ FirebaseAuth: [] }],
        responses: {
          200: { description: 'List of devices with live connection status' },
        },
      },
    },
    '/devices/{deviceId}': {
      get: {
        summary: 'Get Device Details',
        security: [{ FirebaseAuth: [] }],
        parameters: [{ in: 'path', name: 'deviceId', required: true, schema: { type: 'string' } }],
        responses: { 200: { description: 'Device details' } },
      },
      delete: {
        summary: 'Remove Device',
        security: [{ FirebaseAuth: [] }],
        parameters: [{ in: 'path', name: 'deviceId', required: true, schema: { type: 'string' } }],
        responses: { 200: { description: 'Device deleted' } },
      },
    },
    '/devices/{deviceId}/heartbeat': {
      post: {
        summary: 'Device Heartbeat',
        security: [{ DeviceIdHeader: [], DeviceTokenAuth: [] }],
        parameters: [{ in: 'path', name: 'deviceId', required: true, schema: { type: 'string' } }],
        responses: { 200: { description: 'Heartbeat acknowledged' } },
      },
    },
    '/devices/{deviceId}/sims/sync': {
      post: {
        summary: 'Sync SIM metadata after device approval',
        description: 'Only the matching device can sync its own SIM metadata. SIM verification remains disabled.',
        security: [{ DeviceIdHeader: [], DeviceTokenAuth: [] }],
        parameters: [{ in: 'path', name: 'deviceId', required: true, schema: { type: 'string' } }],
        responses: { 200: { description: 'SIM metadata synchronized' }, 403: { description: 'Device ownership mismatch' } },
      },
    },
    '/devices/{deviceId}/sims': {
      get: {
        summary: 'List SIMs on Device',
        security: [{ FirebaseAuth: [] }],
        parameters: [{ in: 'path', name: 'deviceId', required: true, schema: { type: 'string' } }],
        responses: { 200: { description: 'List of SIM slots and statuses' } },
      },
    },
    '/devices/{deviceId}/sims/{simId}': {
      patch: {
        summary: 'Update SIM Verification & Fallback Authorization',
        description: 'Explicitly verifies a SIM card and authorizes/revokes its usage as an Auto Switch fallback.',
        security: [{ FirebaseAuth: [] }],
        parameters: [
          { in: 'path', name: 'deviceId', required: true, schema: { type: 'string' } },
          { in: 'path', name: 'simId', required: true, schema: { type: 'string' } },
        ],
        requestBody: {
          required: true,
          content: {
            'application/json': {
              schema: {
                type: 'object',
                properties: {
                  isVerified: { type: 'boolean', example: true },
                  isAuthorizedFallback: { type: 'boolean', example: true },
                  displayName: { type: 'string', example: 'Jio Primary' },
                  phoneNumber: { type: 'string', example: '+919876543210' },
                },
              },
            },
          },
        },
        responses: { 200: { description: 'SIM authorization updated successfully' } },
      },
    },
    '/bots': {
      get: {
        summary: 'List Bots',
        security: [{ FirebaseAuth: [] }],
        responses: { 200: { description: 'List of bots' } },
      },
      post: {
        summary: 'Create Bot',
        security: [{ FirebaseAuth: [] }],
        requestBody: {
          required: true,
          content: {
            'application/json': { schema: { $ref: '#/components/schemas/CreateBotRequest' } },
          },
        },
        responses: { 201: { description: 'Bot created' } },
      },
    },
    '/bots/{botId}': {
      get: {
        summary: 'Get Bot Details',
        security: [{ FirebaseAuth: [] }],
        parameters: [{ in: 'path', name: 'botId', required: true, schema: { type: 'string' } }],
        responses: { 200: { description: 'Bot configuration' } },
      },
      patch: {
        summary: 'Update Bot',
        security: [{ FirebaseAuth: [] }],
        parameters: [{ in: 'path', name: 'botId', required: true, schema: { type: 'string' } }],
        responses: { 200: { description: 'Bot updated' } },
      },
      delete: {
        summary: 'Delete Bot',
        security: [{ FirebaseAuth: [] }],
        parameters: [{ in: 'path', name: 'botId', required: true, schema: { type: 'string' } }],
        responses: { 200: { description: 'Bot removed' } },
      },
    },
    '/sms/send': {
      post: {
        summary: 'Send SMS Message',
        description:
          'Primary endpoint to send SMS. Verifies Bot, Device, SIM assignment, enforces idempotency, ' +
          'stores persistent job in Firestore, and pushes command immediately over WebSocket if phone is connected.',
        security: [{ ApiKeyAuth: [] }, { FirebaseAuth: [] }],
        requestBody: {
          required: true,
          content: {
            'application/json': { schema: { $ref: '#/components/schemas/SendSmsRequest' } },
          },
        },
        responses: {
          200: { description: 'Command dispatched to connected Android phone' },
          202: { description: 'Command persisted in queue; will dispatch when phone connects' },
          400: { description: 'SIM ambiguous or validation error' },
          429: { description: 'Rate limit exceeded' },
        },
      },
    },
    '/sms/logs': {
      get: {
        summary: 'Get SMS Logs',
        security: [{ ApiKeyAuth: [] }, { FirebaseAuth: [] }],
        parameters: [
          { in: 'query', name: 'bot_id', schema: { type: 'string' } },
          { in: 'query', name: 'device_id', schema: { type: 'string' } },
          { in: 'query', name: 'direction', schema: { type: 'string', enum: ['inbound', 'outbound'] } },
          { in: 'query', name: 'limit', schema: { type: 'integer', default: 50 } },
        ],
        responses: { 200: { description: 'Message logs' } },
      },
    },
    '/sms/{messageId}': {
      get: {
        summary: 'Get Message Details',
        security: [{ ApiKeyAuth: [] }, { FirebaseAuth: [] }],
        parameters: [{ in: 'path', name: 'messageId', required: true, schema: { type: 'string' } }],
        responses: { 200: { description: 'Message or job details' } },
      },
    },
    '/device/sms/received': {
      post: {
        summary: 'Device: Report Incoming SMS',
        security: [{ DeviceIdHeader: [], DeviceTokenAuth: [] }],
        description: 'Invoked by Android app when phone receives an SMS. Stores message and triggers webhook.',
        responses: { 200: { description: 'Incoming SMS processed' } },
      },
    },
    '/device/sms/result': {
      post: {
        summary: 'Device: Report Send Result',
        security: [{ DeviceIdHeader: [], DeviceTokenAuth: [] }],
        description: 'Invoked by Android app after sending SMS (sent, delivered, failed).',
        responses: { 200: { description: 'Status updated' } },
      },
    },
    '/webhooks': {
      get: {
        summary: 'List Webhooks',
        security: [{ FirebaseAuth: [] }],
        responses: { 200: { description: 'List of webhooks' } },
      },
      post: {
        summary: 'Register Webhook',
        security: [{ FirebaseAuth: [] }],
        responses: { 201: { description: 'Webhook registered' } },
      },
    },
    '/webhooks/{webhookId}': {
      patch: {
        summary: 'Update Webhook',
        security: [{ FirebaseAuth: [] }],
        parameters: [{ in: 'path', name: 'webhookId', required: true, schema: { type: 'string' } }],
        responses: { 200: { description: 'Webhook updated' } },
      },
      delete: {
        summary: 'Delete Webhook',
        security: [{ FirebaseAuth: [] }],
        parameters: [{ in: 'path', name: 'webhookId', required: true, schema: { type: 'string' } }],
        responses: { 200: { description: 'Webhook deleted' } },
      },
    },
    '/webhooks/test': {
      post: {
        summary: 'Test Webhook Ping',
        security: [{ FirebaseAuth: [] }],
        responses: { 200: { description: 'Test webhook delivered' } },
      },
    },
  },
};
