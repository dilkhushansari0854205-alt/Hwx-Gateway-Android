import { Router, Request, Response } from 'express';
import { firebaseAuth } from '../../middleware/firebaseAuth.ts';
import {
  createWebhookSchema,
  testWebhookSchema,
  updateWebhookSchema,
} from '../../validators/index.ts';
import { WebhookService } from './webhook.service.ts';

const router = Router();

/**
 * POST /v1/webhooks
 * Registers a new webhook destination.
 */
router.post('/', firebaseAuth(), async (req: Request, res: Response): Promise<void> => {
  const validated = createWebhookSchema.parse(req.body);
  const webhook = await WebhookService.createWebhook(req.user!.uid, validated);

  res.status(201).json({
    success: true,
    data: webhook,
    message: 'Webhook endpoint registered successfully',
  });
});

/**
 * GET /v1/webhooks
 * Lists all webhooks registered for the user.
 */
router.get('/', firebaseAuth(), async (req: Request, res: Response): Promise<void> => {
  const hooks = await WebhookService.listWebhooks(req.user!.uid);

  res.status(200).json({
    success: true,
    data: hooks,
  });
});

/**
 * PATCH /v1/webhooks/:webhookId
 * Updates webhook settings (URL, events, active state).
 */
router.patch('/:webhookId', firebaseAuth(), async (req: Request, res: Response): Promise<void> => {
  const { webhookId } = req.params;
  const validated = updateWebhookSchema.parse(req.body);

  const updated = await WebhookService.updateWebhook(req.user!.uid, webhookId, validated);

  res.status(200).json({
    success: true,
    data: updated,
    message: 'Webhook updated successfully',
  });
});

/**
 * DELETE /v1/webhooks/:webhookId
 * Removes a webhook configuration.
 */
router.delete('/:webhookId', firebaseAuth(), async (req: Request, res: Response): Promise<void> => {
  const { webhookId } = req.params;
  const deleted = await WebhookService.deleteWebhook(req.user!.uid, webhookId);

  if (!deleted) {
    res.status(404).json({
      success: false,
      error: { code: 'WEBHOOK_NOT_FOUND', message: 'Webhook not found' },
    });
    return;
  }

  res.status(200).json({
    success: true,
    message: 'Webhook deleted successfully',
  });
});

/**
 * POST /v1/webhooks/test
 * Sends a test ping event to verify connectivity, SSRF protection, and signature generation.
 */
router.post('/test', firebaseAuth(), async (req: Request, res: Response): Promise<void> => {
  const validated = testWebhookSchema.parse(req.body);
  const secret = validated.secret || 'hwx_test_secret_123';

  const testPayload = {
    event: 'test.ping',
    event_id: `evt_test_${Date.now()}`,
    message: 'HWX SMS Gateway Webhook Test Ping',
    timestamp: new Date().toISOString(),
  };

  const result = await WebhookService.sendWebhookRequest(validated.url, secret, testPayload, 0);

  if (!result.success) {
    res.status(400).json({
      success: false,
      error: {
        code: 'WEBHOOK_DELIVERY_FAILED',
        message: result.error || 'Failed to reach webhook URL',
        statusCode: result.statusCode,
      },
    });
    return;
  }

  res.status(200).json({
    success: true,
    data: {
      url: validated.url,
      statusCode: result.statusCode,
      responsePreview: result.responseBody,
    },
    message: 'Test ping delivered successfully',
  });
});

export const webhookRoutes = router;
