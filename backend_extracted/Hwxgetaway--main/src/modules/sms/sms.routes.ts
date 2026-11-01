import { Router, Request, Response } from 'express';
import { combinedAuth } from '../../middleware/apiKeyAuth.ts';
import { smsRateLimiter } from '../../middleware/rateLimit.ts';
import { sendSmsSchema } from '../../validators/index.ts';
import { SmsService } from './sms.service.ts';

const router = Router();

/**
 * POST /v1/sms/send
 * Primary SMS Dispatch API for external systems and frontend clients.
 * Authenticates via API Key (X-API-Key or Bearer hwx_live_...) or Firebase ID Token.
 */
router.post(
  '/send',
  combinedAuth,
  smsRateLimiter,
  async (req: Request, res: Response): Promise<void> => {
    const validated = sendSmsSchema.parse(req.body);

    // Support Idempotency-Key header or body field
    const idempotencyKey =
      (req.header('idempotency-key') as string) ||
      (req.header('x-idempotency-key') as string) ||
      validated.idempotency_key;

    const job = await SmsService.sendSms(req.user!.uid, {
      ...validated,
      idempotency_key: idempotencyKey,
    });

    const isAccepted = job.status === 'queued';

    res.status(isAccepted ? 202 : 200).json({
      success: true,
      data: {
        jobId: job.id,
        botId: job.botId,
        deviceId: job.deviceId,
        simId: job.simId,
        simSlot: job.simSlot,
        to: job.to,
        status: job.status,
        createdAt: job.createdAt,
        dispatchedAt: job.dispatchedAt,
      },
      message:
        job.status === 'sending'
          ? 'SMS command dispatched to device'
          : 'SMS job safely queued in database; will dispatch when device is online',
    });
  }
);

/**
 * GET /v1/sms/logs
 * Retrieves sent and received SMS message history.
 */
router.get('/logs', combinedAuth, async (req: Request, res: Response): Promise<void> => {
  const { bot_id, device_id, direction, limit } = req.query;

  const logs = await SmsService.listSmsLogs(req.user!.uid, {
    botId: bot_id ? String(bot_id) : undefined,
    deviceId: device_id ? String(device_id) : undefined,
    direction: direction === 'inbound' || direction === 'outbound' ? direction : undefined,
    limit: limit ? parseInt(String(limit), 10) : 50,
  });

  res.status(200).json({
    success: true,
    data: logs,
  });
});

/**
 * GET /v1/sms/:messageId
 * Retrieves status and details of an individual SMS job or message.
 */
router.get('/:messageId', combinedAuth, async (req: Request, res: Response): Promise<void> => {
  const { messageId } = req.params;
  const item = await SmsService.getSmsById(req.user!.uid, messageId);

  if (!item) {
    res.status(404).json({
      success: false,
      error: { code: 'SMS_NOT_FOUND', message: 'SMS record or job not found' },
    });
    return;
  }

  res.status(200).json({
    success: true,
    data: item,
  });
});

export const smsRoutes = router;
