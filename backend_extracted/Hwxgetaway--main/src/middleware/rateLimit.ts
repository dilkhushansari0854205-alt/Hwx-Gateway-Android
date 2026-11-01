import rateLimit from 'express-rate-limit';
import { Request, Response } from 'express';
import { config } from '../config/env.ts';

/**
 * HWX SMS Gateway - Rate Limiting Middlewares
 */

// Global API rate limiter
export const globalRateLimiter = rateLimit({
  windowMs: 15 * 60 * 1000, // 15 minutes
  max: 1000,
  standardHeaders: true,
  legacyHeaders: false,
  message: {
    success: false,
    error: {
      code: 'RATE_LIMIT_EXCEEDED',
      message: 'Too many requests from this IP. Please try again later.',
    },
  },
});

// SMS Sending rate limiter (keyed by User UID or API Key)
export const smsRateLimiter = rateLimit({
  windowMs: 60 * 1000, // 1 minute
  max: config.MAX_SMS_PER_MINUTE || 60,
  keyGenerator: (req: Request) => req.user?.uid || req.apiKey?.id || req.ip || 'anonymous',
  validate: false,
  standardHeaders: true,
  legacyHeaders: false,
  handler: (_req: Request, res: Response) => {
    res.status(429).json({
      success: false,
      error: {
        code: 'SMS_RATE_LIMIT_EXCEEDED',
        message: `Exceeded maximum rate limit of ${config.MAX_SMS_PER_MINUTE} SMS messages per minute`,
      },
    });
  },
});

// API Key generation limiter
export const apiKeyRateLimiter = rateLimit({
  windowMs: 60 * 60 * 1000, // 1 hour
  max: 20,
  keyGenerator: (req: Request) => req.user?.uid || req.ip || 'anonymous',
  validate: false,
  standardHeaders: true,
  legacyHeaders: false,
  message: {
    success: false,
    error: {
      code: 'API_KEY_RATE_LIMIT_EXCEEDED',
      message: 'Too many API keys generated in a short period',
    },
  },
});

// Device Heartbeat rate limiter
export const deviceHeartbeatLimiter = rateLimit({
  windowMs: 60 * 1000, // 1 minute
  max: 120, // Allows up to 2 heartbeats/second per device
  keyGenerator: (req: Request) => req.device?.id || req.header('x-device-id') || req.ip || 'device',
  validate: false,
  standardHeaders: true,
  legacyHeaders: false,
  message: {
    success: false,
    error: {
      code: 'DEVICE_HEARTBEAT_THROTTLED',
      message: 'Heartbeat rate too high',
    },
  },
});


// Limits pending device approval requests to reduce account-spam and notification abuse.
export const deviceEnrollmentLimiter = rateLimit({
  windowMs: 10 * 60 * 1000,
  max: 5,
  keyGenerator: (req: Request) => req.user?.uid || req.ip || 'anonymous',
  validate: false,
  standardHeaders: true,
  legacyHeaders: false,
  message: {
    success: false,
    error: {
      code: 'DEVICE_ENROLLMENT_RATE_LIMITED',
      message: 'Too many device approval requests. Please wait before trying again.',
    },
  },
});
