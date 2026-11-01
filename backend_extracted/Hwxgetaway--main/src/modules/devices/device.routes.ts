import { Router, Request, Response } from 'express';
import { firebaseAuth } from '../../middleware/firebaseAuth.ts';
import { deviceAuth } from '../../middleware/deviceAuth.ts';
import { deviceEnrollmentLimiter, deviceHeartbeatLimiter } from '../../middleware/rateLimit.ts';
import { createDeviceEnrollmentSchema, heartbeatSchema, registerDeviceSchema, syncDeviceSimsSchema, updateSimSchema } from '../../validators/index.ts';
import { DeviceService } from './device.service.ts';
import { DeviceSocketManager } from '../websocket/deviceSocket.ts';

const router = Router();

/**
 * POST /v1/devices/register
 * Registers a new Android device under the authenticated user.
 * Returns raw deviceToken once.
 */
router.post('/register', firebaseAuth(), async (req: Request, res: Response): Promise<void> => {
  const validated = registerDeviceSchema.parse(req.body);
  try {
    const result = await DeviceService.registerDevice(req.user!.uid, validated);
    res.status(201).json({
      success: true,
      data: {
        device: {
          id: result.device.id,
          name: result.device.name,
          androidVersion: result.device.androidVersion,
          appVersion: result.device.appVersion,
          status: result.device.status,
          sims: result.device.sims,
          registeredAt: result.device.registeredAt,
        },
        deviceToken: result.rawDeviceToken,
      },
      message: 'First trusted device registered successfully.',
    });
  } catch (error) {
    if (error instanceof Error && error.message === 'DEVICE_APPROVAL_REQUIRED') {
      res.status(409).json({
        success: false,
        error: { code: 'DEVICE_APPROVAL_REQUIRED', message: 'An existing trusted device must approve this phone.' },
      });
      return;
    }
    throw error;
  }
});

/** Create a pending request from a new phone. It receives no device token yet. */
router.post('/enrollment-requests', firebaseAuth(), deviceEnrollmentLimiter, async (req: Request, res: Response): Promise<void> => {
  const validated = createDeviceEnrollmentSchema.parse(req.body);
  try {
    const result = await DeviceService.createEnrollmentRequest(req.user!.uid, validated);
    res.status(202).json({ success: true, data: result, message: 'Approval request sent to your existing trusted devices.' });
  } catch (error) {
    if (error instanceof Error && error.message === 'FIRST_DEVICE_REQUIRED') {
      res.status(409).json({ success: false, error: { code: 'FIRST_DEVICE_REQUIRED', message: 'Register the first trusted phone before requesting another device.' } });
      return;
    }
    throw error;
  }
});

/** Pending requests can only be read by an already trusted device. */
router.get('/enrollment-requests/pending', deviceAuth, async (req: Request, res: Response): Promise<void> => {
  const data = await DeviceService.listPendingEnrollments(req.user!.uid);
  res.status(200).json({ success: true, data });
});

/** New phone polls with its Firebase account; token is issued only after approval. */
router.get('/enrollment-requests/:requestId', firebaseAuth(), async (req: Request, res: Response): Promise<void> => {
  const result = await DeviceService.getEnrollmentStatus(req.user!.uid, req.params.requestId);
  if (!result) {
    res.status(404).json({ success: false, error: { code: 'ENROLLMENT_NOT_FOUND', message: 'Enrollment request not found.' } });
    return;
  }
  res.status(200).json({ success: true, data: result });
});

router.post('/enrollment-requests/:requestId/approve', deviceAuth, async (req: Request, res: Response): Promise<void> => {
  const approved = await DeviceService.decideEnrollment(req.user!.uid, req.params.requestId, req.device!.id, 'approved');
  if (!approved) {
    res.status(409).json({ success: false, error: { code: 'ENROLLMENT_NOT_PENDING', message: 'Request is expired or has already been handled.' } });
    return;
  }
  res.status(200).json({ success: true, message: 'New device approved. It can now complete registration.' });
});

router.post('/enrollment-requests/:requestId/reject', deviceAuth, async (req: Request, res: Response): Promise<void> => {
  const rejected = await DeviceService.decideEnrollment(req.user!.uid, req.params.requestId, req.device!.id, 'rejected');
  if (!rejected) {
    res.status(409).json({ success: false, error: { code: 'ENROLLMENT_NOT_PENDING', message: 'Request is expired or has already been handled.' } });
    return;
  }
  res.status(200).json({ success: true, message: 'New device request rejected.' });
});

/**
 * GET /v1/devices
 * Lists all registered devices for the user with live connection status.
 */
router.get('/', firebaseAuth(), async (req: Request, res: Response): Promise<void> => {
  const devices = await DeviceService.listDevices(req.user!.uid);
  const socketManager = DeviceSocketManager.getInstance();

  const enriched = devices.map((d) => ({
    ...d,
    isSocketConnected: socketManager.isDeviceConnected(d.id),
  }));

  res.status(200).json({
    success: true,
    data: enriched,
  });
});

/**
 * GET /v1/devices/:deviceId
 * Retrieves details for a specific device.
 */
router.get('/:deviceId', firebaseAuth(), async (req: Request, res: Response): Promise<void> => {
  const { deviceId } = req.params;
  const device = await DeviceService.getDevice(req.user!.uid, deviceId);

  if (!device) {
    res.status(404).json({
      success: false,
      error: { code: 'DEVICE_NOT_FOUND', message: 'Device not found' },
    });
    return;
  }

  const isConnected = DeviceSocketManager.getInstance().isDeviceConnected(deviceId);

  res.status(200).json({
    success: true,
    data: {
      ...device,
      isSocketConnected: isConnected,
    },
  });
});

/**
 * DELETE /v1/devices/:deviceId
 * Unregisters and removes a device.
 */
router.delete('/:deviceId', firebaseAuth(), async (req: Request, res: Response): Promise<void> => {
  const { deviceId } = req.params;
  const deleted = await DeviceService.deleteDevice(req.user!.uid, deviceId);

  if (!deleted) {
    res.status(404).json({
      success: false,
      error: { code: 'DEVICE_NOT_FOUND', message: 'Device not found' },
    });
    return;
  }

  res.status(200).json({
    success: true,
    message: 'Device unregistered successfully',
  });
});

/**
 * POST /v1/devices/:deviceId/heartbeat
 * Heartbeat endpoint for devices communicating over HTTP.
 */
router.post(
  '/:deviceId/heartbeat',
  deviceHeartbeatLimiter,
  async (req: Request, res: Response): Promise<void> => {
    const { deviceId } = req.params;
    const validated = heartbeatSchema.parse(req.body);

    // Can be authenticated via X-Device-Token or Bearer Token
    const deviceToken = req.header('x-device-token') || req.header('authorization')?.replace('Bearer ', '');
    if (!deviceToken) {
      res.status(401).json({
        success: false,
        error: { code: 'AUTH_REQUIRED', message: 'Device token required in X-Device-Token or Bearer' },
      });
      return;
    }

    const auth = await DeviceService.authenticateDevice(deviceId, deviceToken);
    if (!auth) {
      res.status(401).json({
        success: false,
        error: { code: 'DEVICE_AUTH_FAILED', message: 'Invalid device credentials' },
      });
      return;
    }

    await DeviceService.updateHeartbeat(auth.uid, deviceId, {
      batteryLevel: validated.batteryLevel,
      isCharging: validated.isCharging,
      networkType: validated.networkType,
      ipAddress: req.ip,
    });

    res.status(200).json({
      success: true,
      data: {
        deviceId,
        status: 'online',
        timestamp: new Date().toISOString(),
      },
    });
  }
);

/** Sync SIM metadata after a newly approved phone has received its device credentials. */
router.post('/:deviceId/sims/sync', deviceAuth, async (req: Request, res: Response): Promise<void> => {
  const { deviceId } = req.params;
  if (req.device!.id !== deviceId) {
    res.status(403).json({ success: false, error: { code: 'DEVICE_OWNERSHIP_MISMATCH', message: 'A device can only sync its own SIM metadata.' } });
    return;
  }
  const validated = syncDeviceSimsSchema.parse(req.body);
  const sims = await DeviceService.syncDeviceSims(req.user!.uid, deviceId, validated.sims);
  res.status(200).json({ success: true, data: sims, message: 'SIM metadata synced. SIM authorization remains disabled until explicitly verified.' });
});

/**
 * GET /v1/devices/:deviceId/sims
 * Lists all SIM cards registered to this device.
 */
router.get('/:deviceId/sims', firebaseAuth(), async (req: Request, res: Response): Promise<void> => {
  const { deviceId } = req.params;
  const device = await DeviceService.getDevice(req.user!.uid, deviceId);

  if (!device) {
    res.status(404).json({
      success: false,
      error: { code: 'DEVICE_NOT_FOUND', message: 'Device not found' },
    });
    return;
  }

  const sims = await DeviceService.listDeviceSims(req.user!.uid, deviceId);

  res.status(200).json({
    success: true,
    data: sims,
  });
});

/**
 * PATCH /v1/devices/:deviceId/sims/:simId
 * Authorize or update SIM verification status and Auto Switch fallback authorization.
 */
router.patch('/:deviceId/sims/:simId', firebaseAuth(), async (req: Request, res: Response): Promise<void> => {
  const { deviceId, simId } = req.params;
  const device = await DeviceService.getDevice(req.user!.uid, deviceId);

  if (!device) {
    res.status(404).json({
      success: false,
      error: { code: 'DEVICE_NOT_FOUND', message: 'Device not found' },
    });
    return;
  }

  const validated = updateSimSchema.parse(req.body);
  const updatedSim = await DeviceService.updateSimAuthorization(req.user!.uid, deviceId, simId, validated);

  res.status(200).json({
    success: true,
    data: updatedSim,
    message: 'SIM authorization updated successfully',
  });
});

export const deviceRoutes = router;
