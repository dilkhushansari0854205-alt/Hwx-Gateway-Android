import { Request, Response, NextFunction } from 'express';
import { ApiKeyService } from '../modules/api-keys/apiKey.service.ts';
import { UserService } from '../modules/users/user.service.ts';

/**
 * HWX SMS Gateway - API Key Authentication Middleware
 *
 * Verifies API keys sent via 'X-API-Key' or 'Authorization: Bearer hwx_live_...'
 */
export async function apiKeyAuth(req: Request, res: Response, next: NextFunction): Promise<void> {
  try {
    let rawKey = req.header('x-api-key');

    if (!rawKey) {
      const authHeader = req.header('authorization');
      if (authHeader && authHeader.startsWith('Bearer hwx_live_')) {
        rawKey = authHeader.replace('Bearer ', '').trim();
      }
    }

    if (!rawKey) {
      res.status(401).json({
        success: false,
        error: {
          code: 'API_KEY_MISSING',
          message: 'API key is required. Pass via X-API-Key header or Authorization: Bearer hwx_live_...',
        },
      });
      return;
    }

    const authResult = await ApiKeyService.validateRawApiKey(rawKey);

    if (!authResult) {
      res.status(401).json({
        success: false,
        error: {
          code: 'API_KEY_INVALID',
          message: 'The provided API key is invalid, revoked, or expired',
        },
      });
      return;
    }

    const user = await UserService.getUserById(authResult.uid);

    if (!user) {
      res.status(401).json({
        success: false,
        error: {
          code: 'USER_NOT_FOUND',
          message: 'Owner of this API key was not found',
        },
      });
      return;
    }

    if (user.status === 'suspended') {
      res.status(403).json({
        success: false,
        error: {
          code: 'ACCOUNT_SUSPENDED',
          message: 'Your account is suspended',
        },
      });
      return;
    }

    req.user = user;
    req.apiKey = authResult.apiKey;
    next();
  } catch (err: unknown) {
    const message = err instanceof Error ? err.message : 'Authentication failed';
    res.status(500).json({
      success: false,
      error: {
        code: 'AUTH_INTERNAL_ERROR',
        message: 'Internal error during API key authentication',
      },
    });
  }
}

/**
 * Flexible Authenticator: Accepts either Firebase ID Token OR API Key.
 * Useful for SMS dispatching which can be called from dashboard UI or external systems.
 */
export async function combinedAuth(req: Request, res: Response, next: NextFunction): Promise<void> {
  const xApiKey = req.header('x-api-key');
  const authHeader = req.header('authorization');

  if (xApiKey || (authHeader && authHeader.startsWith('Bearer hwx_live_'))) {
    return apiKeyAuth(req, res, next);
  }

  // Otherwise delegate to firebaseAuth
  const { firebaseAuth } = await import('./firebaseAuth.ts');
  return firebaseAuth()(req, res, next);
}
