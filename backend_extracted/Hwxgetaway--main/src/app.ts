import express, { Request, Response } from 'express';
import helmet from 'helmet';
import cors from 'cors';
import swaggerUi from 'swagger-ui-express';
import { config } from './config/env.ts';
import { globalRateLimiter } from './middleware/rateLimit.ts';
import { errorHandler } from './middleware/errorHandler.ts';
import { authRoutes } from './modules/auth/auth.routes.ts';
import { apiKeyRoutes } from './modules/api-keys/apiKey.routes.ts';
import { deviceRoutes } from './modules/devices/device.routes.ts';
import { botRoutes } from './modules/bots/bot.routes.ts';
import { smsRoutes } from './modules/sms/sms.routes.ts';
import { deviceSmsRoutes } from './modules/sms/deviceSms.routes.ts';
import { webhookRoutes } from './modules/webhooks/webhook.routes.ts';
import { swaggerDocument } from './swagger/swagger.ts';
import { getFirestore } from './config/firebase.ts';

export const app = express();

// 1. Security Headers via Helmet
app.use(
  helmet({
    contentSecurityPolicy: false, // Allows Swagger UI and dashboard rendering
    crossOriginEmbedderPolicy: false,
  })
);

// 2. CORS Configuration
app.use(
  cors({
    origin: config.CORS_ORIGIN || '*',
    methods: ['GET', 'POST', 'PATCH', 'PUT', 'DELETE', 'OPTIONS'],
    allowedHeaders: [
      'Content-Type',
      'Authorization',
      'X-API-Key',
      'X-Device-Id',
      'X-Device-Token',
      'Idempotency-Key',
      'X-Idempotency-Key',
    ],
  })
);

// 3. Body Parsing
app.use(express.json({ limit: '1mb' }));
app.use(express.urlencoded({ extended: true, limit: '1mb' }));

// 4. Global Rate Limiter
app.use(globalRateLimiter);

// 5. Request Audit Logger
app.use((req: Request, res: Response, next) => {
  const start = Date.now();
  res.on('finish', () => {
    const duration = Date.now() - start;
    if (req.path !== '/health') {
      console.log(
        `[AUDIT] ${new Date().toISOString()} | ${req.method} ${req.originalUrl} | Status: ${res.statusCode} | ${duration}ms | IP: ${req.ip}`
      );
    }
  });
  next();
});

// 6. Swagger API Documentation
app.use('/docs', swaggerUi.serve, swaggerUi.setup(swaggerDocument));
app.use('/api-docs', swaggerUi.serve, swaggerUi.setup(swaggerDocument));

// 7. System Health Endpoint
app.get('/health', async (_req: Request, res: Response) => {
  let firestoreStatus = 'unknown';
  try {
    const db = getFirestore();
    if (db) {
      firestoreStatus = 'connected';
    }
  } catch (err: unknown) {
    firestoreStatus = 'error: ' + (err instanceof Error ? err.message : String(err));
  }

  const memory = process.memoryUsage();

  res.status(200).json({
    status: 'healthy',
    service: 'HWX SMS Gateway Backend',
    version: '1.0.0',
    environment: config.NODE_ENV,
    timestamp: new Date().toISOString(),
    uptimeSeconds: Math.floor(process.uptime()),
    firestore: firestoreStatus,
    memoryUsageMB: {
      rss: Math.round(memory.rss / 1024 / 1024),
      heapTotal: Math.round(memory.heapTotal / 1024 / 1024),
      heapUsed: Math.round(memory.heapUsed / 1024 / 1024),
    },
    documentation: '/docs',
  });
});

// 8. Service Root Info
app.get('/', (_req: Request, res: Response) => {
  res.status(200).json({
    name: 'HWX SMS Gateway API Server',
    description: 'Production-ready multi-user Android SMS Gateway',
    version: '1.0.0',
    documentation: '/docs',
    health: '/health',
    websocketEndpoint: '/ws/device',
    endpoints: {
      auth: '/v1/auth/me',
      apiKeys: '/v1/api-keys',
      devices: '/v1/devices',
      deviceEnrollment: '/v1/devices/enrollment-requests',
      bots: '/v1/bots',
      sms: '/v1/sms/send',
      smsLogs: '/v1/sms/logs',
      webhooks: '/v1/webhooks',
    },
  });
});

// 9. API Modules Mounting
app.use('/v1/auth', authRoutes);
app.use('/v1/api-keys', apiKeyRoutes);
app.use('/v1/devices', deviceRoutes);
app.use('/v1/bots', botRoutes);
app.use('/v1/sms', smsRoutes);
app.use('/v1/device/sms', deviceSmsRoutes);
app.use('/v1/webhooks', webhookRoutes);

// 10. 404 Catch-All
app.use((req: Request, res: Response) => {
  res.status(404).json({
    success: false,
    error: {
      code: 'ROUTE_NOT_FOUND',
      message: `Cannot ${req.method} ${req.originalUrl}. Refer to /docs for interactive documentation.`,
    },
  });
});

// 11. Centralized Error Handler
app.use(errorHandler);
