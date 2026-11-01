import { Router, Request, Response } from 'express';
import { deviceAuth } from '../../middleware/deviceAuth.ts';
import { deviceIncomingSmsSchema, deviceSmsResultSchema } from '../../validators/index.ts';
import { DeviceSocketManager } from '../websocket/deviceSocket.ts';

const router = Router();

/**
 * POST /v1/device/sms/received
 * Forwarded incoming SMS received on an Android SIM slot.
 * Authenticated via X-Device-Id and X-Device-Token.
 */
router.post('/received', deviceAuth, async (req: Request, res: Response): Promise<void> => {
  const validated = deviceIncomingSmsSchema.parse(req.body);
  const socketManager = DeviceSocketManager.getInstance();

  await socketManager.handleIncomingSms(req.user!.uid, req.device!.id, {
    type: 'SMS_INCOMING',
    deviceId: req.device!.id,
    simSlot: validated.simSlot,
    subscriptionId: validated.subscriptionId,
    from: validated.from,
    message: validated.message,
    timestamp: validated.timestamp || new Date().toISOString(),
  });

  res.status(200).json({
    success: true,
    message: 'Incoming SMS processed, stored, and webhook dispatched',
  });
});

/**
 * POST /v1/device/sms/result
 * Android device reports SMS delivery outcome (sent, delivered, failed).
 * Authenticated via X-Device-Id and X-Device-Token.
 */
router.post('/result', deviceAuth, async (req: Request, res: Response): Promise<void> => {
  const validated = deviceSmsResultSchema.parse(req.body);
  const socketManager = DeviceSocketManager.getInstance();

  await socketManager.handleSmsResult(req.user!.uid, {
    type: 'SMS_RESULT',
    jobId: validated.jobId,
    deviceId: req.device!.id,
    status: validated.status,
    errorCode: validated.errorCode,
    errorMessage: validated.errorMessage,
    timestamp: validated.timestamp || new Date().toISOString(),
  });

  res.status(200).json({
    success: true,
    message: 'SMS delivery status updated successfully',
  });
});

export const deviceSmsRoutes = router;
