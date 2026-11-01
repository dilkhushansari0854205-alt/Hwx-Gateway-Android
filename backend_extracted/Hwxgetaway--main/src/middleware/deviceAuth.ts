import { Request, Response, NextFunction } from 'express';
import { DeviceService } from '../modules/devices/device.service.ts';

/**
 * HWX SMS Gateway - Android Device Authentication Middleware
 *
 * Verifies requests coming from registered Android devices using
 * X-Device-Id and X-Device-Token headers.
 */
export async function deviceAuth(req: Request, res: Response, next: NextFunction): Promise<void> {
  try {
    const deviceId = req.header('x-device-id');
    let rawToken = req.header('x-device-token');

    if (!rawToken) {
      const authHeader = req.header('authorization');
      if (authHeader && authHeader.startsWith('Bearer hwx_dev_')) {
        rawToken = authHeader.replace('Bearer ', '').trim();
      }
    }

    if (!deviceId || !rawToken) {
      res.status(401).json({
        success: false,
        error: {
          code: 'DEVICE_CREDENTIALS_MISSING',
          message: 'Device authentication requires X-Device-Id and X-Device-Token headers',
        },
      });
      return;
    }

    const authResult = await DeviceService.authenticateDevice(deviceId, rawToken);

    if (!authResult) {
      res.status(401).json({
        success: false,
        error: {
          code: 'DEVICE_AUTH_FAILED',
          message: 'Invalid Device ID or Device Token',
        },
      });
      return;
    }

    req.device = authResult.device;
    // Also inject synthetic user object containing uid to reuse ownership utilities
    req.user = {
      uid: authResult.uid,
      email: '',
      emailVerified: true,
      status: 'active',
      createdAt: '',
      updatedAt: '',
      stats: {
        smsSentCount: 0,
        smsReceivedCount: 0,
        apiRequestsCount: 0,
        activeDevicesCount: 1,
      },
    };

    next();
  } catch (err: unknown) {
    const message = err instanceof Error ? err.message : 'Internal error during device authentication';
    res.status(500).json({
      success: false,
      error: {
        code: 'DEVICE_AUTH_INTERNAL_ERROR',
        message,
      },
    });
  }
}
