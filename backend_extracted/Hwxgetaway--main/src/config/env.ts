import { z } from 'zod';
import dotenv from 'dotenv';

// Load environment variables from .env if present
dotenv.config();

const envSchema = z.object({
  PORT: z.string().default('3000').transform((val) => parseInt(val, 10)),
  NODE_ENV: z.enum(['development', 'production', 'test']).default('development'),
  FIREBASE_SERVICE_ACCOUNT: z.string().optional(),
  WEBHOOK_TIMEOUT_MS: z.string().default('10000').transform((val) => parseInt(val, 10)),
  MAX_SMS_PER_MINUTE: z.string().default('60').transform((val) => parseInt(val, 10)),
  CORS_ORIGIN: z.string().default('*'),
});

const parsedEnv = envSchema.safeParse(process.env);

if (!parsedEnv.success) {
  // Redact secrets in error logging
  const issues = parsedEnv.error.issues.map((issue) => ({
    path: issue.path.join('.'),
    message: issue.message,
  }));
  console.error('[CRITICAL] Environment variable validation failed:', JSON.stringify(issues, null, 2));
  if (process.env.NODE_ENV === 'production') {
    process.exit(1);
  }
}

export const config = parsedEnv.success
  ? parsedEnv.data
  : {
      PORT: parseInt(process.env.PORT || '3000', 10),
      NODE_ENV: (process.env.NODE_ENV || 'development') as 'development' | 'production' | 'test',
      FIREBASE_SERVICE_ACCOUNT: process.env.FIREBASE_SERVICE_ACCOUNT,
      WEBHOOK_TIMEOUT_MS: 10000,
      MAX_SMS_PER_MINUTE: 60,
      CORS_ORIGIN: '*',
    };
