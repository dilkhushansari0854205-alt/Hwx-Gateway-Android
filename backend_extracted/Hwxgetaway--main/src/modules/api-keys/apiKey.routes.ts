import { Router, Request, Response } from 'express';
import { firebaseAuth } from '../../middleware/firebaseAuth.ts';
import { apiKeyRateLimiter } from '../../middleware/rateLimit.ts';
import { createApiKeySchema } from '../../validators/index.ts';
import { ApiKeyService } from './apiKey.service.ts';

const router = Router();

/**
 * POST /v1/api-keys
 * Generates a new API key for the authenticated user.
 * Returns raw key once.
 */
router.post(
  '/',
  firebaseAuth(),
  apiKeyRateLimiter,
  async (req: Request, res: Response): Promise<void> => {
    const validated = createApiKeySchema.parse(req.body);
    const result = await ApiKeyService.createApiKey(
      req.user!.uid,
      validated.name,
      validated.expiresAt
    );

    res.status(201).json({
      success: true,
      data: result,
      warning: 'Store the apiKey securely. It will never be shown again.',
    });
  }
);

/**
 * GET /v1/api-keys
 * Lists all active and revoked API keys for the user.
 */
router.get('/', firebaseAuth(), async (req: Request, res: Response): Promise<void> => {
  const keys = await ApiKeyService.listApiKeys(req.user!.uid);
  res.status(200).json({
    success: true,
    data: keys,
  });
});

/**
 * DELETE /v1/api-keys/:keyId
 * Revokes an existing API key.
 */
router.delete('/:keyId', firebaseAuth(), async (req: Request, res: Response): Promise<void> => {
  const { keyId } = req.params;
  const revoked = await ApiKeyService.revokeApiKey(req.user!.uid, keyId);

  if (!revoked) {
    res.status(404).json({
      success: false,
      error: { code: 'KEY_NOT_FOUND', message: 'API key not found' },
    });
    return;
  }

  res.status(200).json({
    success: true,
    message: 'API key revoked successfully',
  });
});

export const apiKeyRoutes = router;
