import { Request, Response, NextFunction } from 'express';
import { ZodError } from 'zod';
import { config } from '../config/env.ts';
import { redactSecrets } from '../services/security.service.ts';

/**
 * HWX SMS Gateway - Centralized Error Handling Middleware
 *
 * Sanitizes errors, formats responses consistently, and protects against
 * stack trace or credential leakage.
 */
export function errorHandler(
  err: unknown,
  req: Request,
  res: Response,
  _next: NextFunction
): void {
  // 1. Zod Validation Errors
  if (err instanceof ZodError) {
    const formattedIssues = err.issues.map((issue) => ({
      field: issue.path.join('.'),
      message: issue.message,
      rule: issue.code,
    }));

    res.status(400).json({
      success: false,
      error: {
        code: 'VALIDATION_ERROR',
        message: 'Request payload validation failed',
        details: formattedIssues,
      },
    });
    return;
  }

  // 2. Syntax / JSON Parsing Errors
  if (err instanceof SyntaxError && 'body' in err) {
    res.status(400).json({
      success: false,
      error: {
        code: 'BAD_REQUEST',
        message: 'Invalid JSON payload received in request body',
      },
    });
    return;
  }

  // 3. Application / Domain Errors
  const errorObj = err instanceof Error ? err : new Error(String(err));
  const message = errorObj.message;

  // Safe error logging with secret redaction
  const sanitizedBody = redactSecrets(req.body);
  console.error(`[API Error] ${req.method} ${req.originalUrl}: ${message}`, {
    body: sanitizedBody,
    user: req.user?.uid,
    device: req.device?.id,
  });

  // Check for common domain error codes
  if (message.startsWith('BOT_NOT_FOUND') || message.startsWith('DEVICE_NOT_FOUND')) {
    res.status(404).json({
      success: false,
      error: {
        code: message.split(':')[0],
        message: message.split(':')[1]?.trim() || message,
      },
    });
    return;
  }

  if (message.startsWith('SIM_') || message.startsWith('DEVICE_') || message.startsWith('BOT_')) {
    const [code, desc] = message.split(':');
    res.status(400).json({
      success: false,
      error: {
        code: code.trim(),
        message: desc ? desc.trim() : code.trim(),
      },
    });
    return;
  }

  // 4. Default Internal Server Error
  const isDev = config.NODE_ENV === 'development';
  res.status(500).json({
    success: false,
    error: {
      code: 'INTERNAL_SERVER_ERROR',
      message: isDev ? message : 'An unexpected internal server error occurred',
      details: isDev ? errorObj.stack : undefined,
    },
  });
}
