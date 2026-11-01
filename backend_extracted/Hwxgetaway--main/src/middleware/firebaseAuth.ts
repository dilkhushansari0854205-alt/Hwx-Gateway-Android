import { Request, Response, NextFunction } from 'express';
import { getAuth } from '../config/firebase.ts';
import { UserService } from '../modules/users/user.service.ts';
import { config } from '../config/env.ts';

/**
 * HWX SMS Gateway - Firebase Authentication Middleware
 *
 * Verifies Firebase ID Tokens from Google Login or Email/Password,
 * extracts UID, automatically creates user profile on first request,
 * and attaches authenticated user object to the request.
 */
export function firebaseAuth(options: { requireEmailVerified?: boolean } = {}) {
  return async (req: Request, res: Response, next: NextFunction): Promise<void> => {
    try {
      const authHeader = req.headers.authorization;

      if (!authHeader || !authHeader.startsWith('Bearer ')) {
        res.status(401).json({
          success: false,
          error: {
            code: 'AUTH_TOKEN_MISSING',
            message: 'Authorization Bearer token is required',
          },
        });
        return;
      }

      const idToken = authHeader.split('Bearer ')[1].trim();

      if (!idToken) {
        res.status(401).json({
          success: false,
          error: {
            code: 'AUTH_TOKEN_EMPTY',
            message: 'Bearer token cannot be empty',
          },
        });
        return;
      }

      let decodedToken: {
        uid: string;
        email?: string;
        name?: string;
        picture?: string;
        email_verified?: boolean;
      };

      // Mock test bypass for unit tests without hitting Google servers
      if (config.NODE_ENV === 'test' && idToken.startsWith('mock_token_')) {
        const uid = idToken.replace('mock_token_', '');
        decodedToken = {
          uid,
          email: `${uid}@test.example.com`,
          name: `Test User ${uid}`,
          email_verified: true,
        };
      } else {
        decodedToken = await getAuth().verifyIdToken(idToken);
      }

      if (options.requireEmailVerified && !decodedToken.email_verified) {
        res.status(403).json({
          success: false,
          error: {
            code: 'EMAIL_NOT_VERIFIED',
            message: 'Email address must be verified to perform this action',
          },
        });
        return;
      }

      // Automatically create or fetch profile
      const user = await UserService.getOrCreateUser(decodedToken);

      if (user.status === 'suspended') {
        res.status(403).json({
          success: false,
          error: {
            code: 'ACCOUNT_SUSPENDED',
            message: 'Your account has been suspended. Please contact support.',
          },
        });
        return;
      }

      req.user = user;
      next();
    } catch (err: unknown) {
      const message = err instanceof Error ? err.message : 'Invalid authentication token';
      res.status(401).json({
        success: false,
        error: {
          code: 'AUTH_TOKEN_INVALID',
          message: 'Failed to verify Firebase authentication token',
          details: config.NODE_ENV === 'development' ? message : undefined,
        },
      });
    }
  };
}
