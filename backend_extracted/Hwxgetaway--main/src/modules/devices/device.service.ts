import crypto from 'crypto';
import { QueryDocumentSnapshot } from 'firebase-admin/firestore';
import { collections, FieldValue } from '../../config/firestore.ts';
import { generateDeviceToken, hashString } from '../../services/security.service.ts';
import { Device, DeviceStatus, SimCard } from '../../types/index.ts';
import { UserService } from '../users/user.service.ts';

/**
 * HWX SMS Gateway - Device Management Service
 *
 * Handles multi-device registration, Dual-SIM persistence,
 * heartbeat tracking, and strict SIM verification authorization.
 */
export class DeviceService {
  /**
   * Registers a new Android device under the authenticated user.
   * Security Requirement: Newly detected SIMs are NOT automatically verified
   * or authorized for Auto Switch fallback to prevent accidental/unauthorized transmission.
   */
  static async registerDevice(
    uid: string,
    data: {
      name: string;
      androidVersion: string;
      appVersion: string;
      sims?: Array<{
        subscriptionId: number;
        slotIndex: number;
        carrierName: string;
        phoneNumber?: string;
        displayName?: string;
      }>;
    }
  ): Promise<{ device: Device; rawDeviceToken: string }> {
    const deviceId = `dev_${crypto.randomBytes(8).toString('hex')}`;
    const { rawToken, tokenHash } = generateDeviceToken();
    const now = new Date().toISOString();

    const normalizedSims: SimCard[] = (data.sims || []).map((sim, index) => {
      const simId = `sim_${deviceId}_slot${sim.slotIndex ?? index}`;
      return {
        id: simId,
        subscriptionId: sim.subscriptionId ?? index + 1,
        slotIndex: sim.slotIndex ?? index,
        carrierName: sim.carrierName || 'Unknown Carrier',
        displayName: sim.displayName || `SIM ${sim.slotIndex + 1}`,
        phoneNumber: sim.phoneNumber || undefined,
        isVerified: false,           // Explicit verification required
        isAuthorizedFallback: false, // Fallback requires explicit user consent
        lastVerifiedAt: undefined,
        createdAt: now,
        updatedAt: now,
      };
    });

    const newDevice: Device = {
      id: deviceId,
      uid,
      name: data.name || 'Android Device',
      androidVersion: data.androidVersion || 'Android',
      appVersion: data.appVersion || '1.0.0',
      status: 'offline', // Becomes online when WebSocket connects or heartbeat is received
      deviceTokenHash: tokenHash,
      lastSeen: now,
      registeredAt: now,
      updatedAt: now,
      sims: normalizedSims,
    };

    // Bootstrap is allowed exactly once per account. Every later device must
    // pass the trusted-device approval flow below. A transaction prevents two
    // simultaneous first logins from both becoming trusted devices.
    const db = collections.users().firestore;
    await db.runTransaction(async (tx) => {
      const bootstrapRef = collections.deviceBootstrapDoc(uid);
      const bootstrap = await tx.get(bootstrapRef);
      const existingDevices = await tx.get(collections.devices(uid).limit(1));
      if (bootstrap.exists || !existingDevices.empty) {
        throw new Error('DEVICE_APPROVAL_REQUIRED');
      }

      tx.create(bootstrapRef, { firstDeviceId: deviceId, createdAt: now });
      tx.create(collections.deviceDoc(uid, deviceId), newDevice);
      tx.create(collections.deviceLookupDoc(deviceId), { uid, deviceId, registeredAt: now });
      for (const sim of normalizedSims) {
        tx.create(collections.simDoc(uid, deviceId, sim.id), sim);
      }
      tx.set(collections.userDoc(uid), {
        stats: { activeDevicesCount: FieldValue.increment(1) },
        updatedAt: now,
      }, { merge: true });
    });

    return {
      device: newDevice,
      rawDeviceToken: rawToken, // Returned ONCE upon registration
    };
  }


  /** Creates a short-lived pending request from a newly authenticated phone. */
  static async createEnrollmentRequest(
    uid: string,
    data: { name: string; androidVersion: string; appVersion: string; sims?: Array<{ subscriptionId: number; slotIndex: number; carrierName: string; phoneNumber?: string; displayName?: string }> }
  ): Promise<{ requestId: string; expiresAt: string }> {
    const trustedDevices = await collections.devices(uid).limit(1).get();
    if (trustedDevices.empty) throw new Error('FIRST_DEVICE_REQUIRED');
    const requestId = `enr_${crypto.randomBytes(12).toString('hex')}`;
    const now = new Date();
    const expiresAt = new Date(now.getTime() + 10 * 60 * 1000).toISOString();
    const request = {
      id: requestId,
      uid,
      name: data.name,
      androidVersion: data.androidVersion,
      appVersion: data.appVersion,
      sims: data.sims || [],
      status: 'pending' as const,
      requestedAt: now.toISOString(),
      expiresAt,
    };
    await collections.deviceEnrollmentRequestDoc(uid, requestId).create(request);
    return { requestId, expiresAt };
  }

  /** Lists pending enrollment requests for the authenticated trusted device owner. */
  static async listPendingEnrollments(uid: string): Promise<Array<Record<string, unknown>>> {
    const snapshot = await collections.deviceEnrollmentRequests(uid).where('status', '==', 'pending').get();
    const now = Date.now();
    const pending: Array<Record<string, unknown>> = [];
    for (const doc of snapshot.docs) {
      const data = doc.data();
      if (Date.parse(String(data.expiresAt || '')) <= now) {
        await doc.ref.update({ status: 'expired', decidedAt: new Date().toISOString() });
        continue;
      }
      // Do not return SIM phone numbers in the approval prompt.
      pending.push({
        id: data.id,
        name: data.name,
        androidVersion: data.androidVersion,
        appVersion: data.appVersion,
        requestedAt: data.requestedAt,
        expiresAt: data.expiresAt,
      });
    }
    return pending;
  }

  static async decideEnrollment(
    uid: string,
    requestId: string,
    trustedDeviceId: string,
    decision: 'approved' | 'rejected'
  ): Promise<boolean> {
    const ref = collections.deviceEnrollmentRequestDoc(uid, requestId);
    return collections.users().firestore.runTransaction(async (tx) => {
      const snap = await tx.get(ref);
      if (!snap.exists) return false;
      const data = snap.data()!;
      if (data.status !== 'pending' || Date.parse(String(data.expiresAt || '')) <= Date.now()) return false;
      tx.update(ref, {
        status: decision,
        approvedByDeviceId: trustedDeviceId,
        decidedAt: new Date().toISOString(),
      });
      return true;
    });
  }

  /**
   * The new phone polls using its Firebase account. The first successful poll
   * after approval atomically creates the device and returns its raw token once.
   */
  static async getEnrollmentStatus(
    uid: string,
    requestId: string
  ): Promise<{ status: string; device?: Device; deviceToken?: string; expiresAt?: string } | null> {
    const db = collections.users().firestore;
    const requestRef = collections.deviceEnrollmentRequestDoc(uid, requestId);
    const requestSnap = await requestRef.get();
    if (!requestSnap.exists) return null;
    const initial = requestSnap.data()!;
    if (initial.status === 'pending' && Date.parse(String(initial.expiresAt || '')) <= Date.now()) {
      await requestRef.update({ status: 'expired', decidedAt: new Date().toISOString() });
      return { status: 'expired' };
    }
    if (initial.status !== 'approved') {
      return { status: String(initial.status), expiresAt: initial.expiresAt };
    }

    const deviceId = `dev_${crypto.randomBytes(8).toString('hex')}`;
    const { rawToken, tokenHash } = generateDeviceToken();
    const now = new Date().toISOString();

    const device: Device = {
      id: deviceId,
      uid,
      name: String(initial.name || 'Android Device'),
      androidVersion: String(initial.androidVersion || 'Android'),
      appVersion: String(initial.appVersion || '1.0.0'),
      status: 'offline',
      deviceTokenHash: tokenHash,
      lastSeen: now,
      registeredAt: now,
      updatedAt: now,
      sims: (Array.isArray(initial.sims) ? initial.sims : []).map((sim: any, index: number) => ({
        id: `sim_${deviceId}_slot${sim.slotIndex ?? index}`,
        subscriptionId: sim.subscriptionId ?? index + 1,
        slotIndex: sim.slotIndex ?? index,
        carrierName: sim.carrierName || 'Unknown Carrier',
        displayName: sim.displayName || `SIM ${(sim.slotIndex ?? index) + 1}`,
        phoneNumber: sim.phoneNumber || undefined,
        isVerified: false,
        isAuthorizedFallback: false,
        createdAt: now,
        updatedAt: now,
      })),
    };

    const result = await db.runTransaction(async (tx) => {
      const snap = await tx.get(requestRef);
      if (!snap.exists) return { status: 'not_found' as const };
      const current = snap.data()!;
      if (current.status !== 'approved') {
        return { status: String(current.status) as 'pending' | 'rejected' | 'expired' | 'consumed' };
      }
      tx.create(collections.deviceDoc(uid, deviceId), device);
      tx.create(collections.deviceLookupDoc(deviceId), { uid, deviceId, registeredAt: now });
      for (const sim of device.sims) tx.create(collections.simDoc(uid, deviceId, sim.id), sim);
      tx.set(collections.userDoc(uid), {
        stats: { activeDevicesCount: FieldValue.increment(1) },
        updatedAt: now,
      }, { merge: true });
      tx.update(requestRef, { status: 'consumed', consumedAt: now, createdDeviceId: deviceId });
      return { status: 'created' as const };
    });

    if (result.status === 'created') return { status: 'approved', device: { ...device, deviceTokenHash: undefined } as unknown as Device, deviceToken: rawToken };
    return { status: result.status };
  }

  /**
   * Authenticates a device by Device ID and raw device token.
   */
  static async authenticateDevice(
    deviceId: string,
    rawToken: string
  ): Promise<{ device: Device; uid: string } | null> {
    if (!deviceId || !rawToken) return null;

    const lookupDoc = await collections.deviceLookupDoc(deviceId).get();
    if (!lookupDoc.exists) {
      return null;
    }

    const { uid } = lookupDoc.data() as { uid: string };
    const deviceDoc = await collections.deviceDoc(uid, deviceId).get();

    if (!deviceDoc.exists) {
      return null;
    }

    const device = deviceDoc.data() as Device;

    if (device.status === 'disabled') {
      return null;
    }

    // Verify token hash
    const expectedHash = hashString(rawToken);
    if (device.deviceTokenHash !== expectedHash) {
      return null;
    }

    return { device, uid };
  }

  /**
   * Lists all devices owned by a user.
   */
  static async listDevices(uid: string): Promise<Omit<Device, 'deviceTokenHash'>[]> {
    const snapshot = await collections.devices(uid).orderBy('registeredAt', 'desc').get();
    return snapshot.docs.map((doc: QueryDocumentSnapshot) => {
      const data = doc.data() as Device;
      const { deviceTokenHash: _unused, ...safeDevice } = data;
      return safeDevice;
    });
  }

  /**
   * Retrieves specific device details.
   */
  static async getDevice(uid: string, deviceId: string): Promise<Omit<Device, 'deviceTokenHash'> | null> {
    const doc = await collections.deviceDoc(uid, deviceId).get();
    if (!doc.exists) return null;
    const data = doc.data() as Device;
    const { deviceTokenHash: _unused, ...safeDevice } = data;
    return safeDevice;
  }

  /**
   * Removes a registered device and cleans up lookup index.
   */
  static async deleteDevice(uid: string, deviceId: string): Promise<boolean> {
    const docRef = collections.deviceDoc(uid, deviceId);
    const doc = await docRef.get();
    if (!doc.exists) return false;

    // Firestore does not cascade-delete subcollections when a parent document is removed.
    const simSnapshot = await collections.sims(uid, deviceId).get();
    const batch = collections.users().firestore.batch();
    for (const simDoc of simSnapshot.docs) batch.delete(simDoc.ref);
    batch.delete(docRef);
    batch.delete(collections.deviceLookupDoc(deviceId));
    await batch.commit();
    await UserService.updateActiveDevicesCount(uid, -1);

    return true;
  }

  /**
   * Updates device heartbeat, battery, network, and marks device online.
   */
  static async updateHeartbeat(
    uid: string,
    deviceId: string,
    payload: {
      batteryLevel?: number;
      isCharging?: boolean;
      networkType?: string;
      ipAddress?: string;
    }
  ): Promise<void> {
    const now = new Date().toISOString();
    await collections.deviceDoc(uid, deviceId).update({
      lastSeen: now,
      status: 'online',
      batteryLevel: payload.batteryLevel ?? null,
      isCharging: payload.isCharging ?? null,
      networkType: payload.networkType ?? null,
      ipAddress: payload.ipAddress ?? null,
      updatedAt: now,
    });
  }

  /**
   * Updates device status (e.g. 'online' or 'offline').
   */
  static async setDeviceStatus(uid: string, deviceId: string, status: DeviceStatus): Promise<void> {
    await collections.deviceDoc(uid, deviceId).update({
      status,
      updatedAt: new Date().toISOString(),
    });
  }

  /**
   * Lists SIM cards associated with a device.
   */
  static async listDeviceSims(uid: string, deviceId: string): Promise<SimCard[]> {
    const snapshot = await collections.sims(uid, deviceId).get();
    return snapshot.docs.map((doc: QueryDocumentSnapshot) => doc.data() as SimCard);
  }

  /**
   * Retrieves a specific SIM on a device.
   */
  static async getDeviceSim(uid: string, deviceId: string, simId: string): Promise<SimCard | null> {
    const doc = await collections.simDoc(uid, deviceId, simId).get();
    if (!doc.exists) return null;
    return doc.data() as SimCard;
  }

  /**
   * Explicitly updates verification status, authorized fallback status, or metadata for a SIM.
   */
  static async updateSimAuthorization(
    uid: string,
    deviceId: string,
    simId: string,
    updates: {
      isVerified?: boolean;
      isAuthorizedFallback?: boolean;
      displayName?: string;
      phoneNumber?: string;
    }
  ): Promise<SimCard> {
    const simRef = collections.simDoc(uid, deviceId, simId);
    const doc = await simRef.get();
    if (!doc.exists) {
      throw new Error(`SIM '${simId}' not found on device '${deviceId}'`);
    }

    const existing = doc.data() as SimCard;
    const now = new Date().toISOString();

    const updatedData: Partial<SimCard> = {
      ...updates,
      lastVerifiedAt: updates.isVerified === true ? now : existing.lastVerifiedAt,
      updatedAt: now,
    };

    await simRef.update(updatedData);

    // Also sync the embedded sims array on the parent device document
    const allSims = await this.listDeviceSims(uid, deviceId);
    await collections.deviceDoc(uid, deviceId).update({
      sims: allSims,
      updatedAt: now,
    });

    return { ...existing, ...updatedData };
  }

  /**
   * Detects SIM changes reported by phone and revokes verification upon carrier/subId changes.
   */
  static async syncDeviceSims(
    uid: string,
    deviceId: string,
    reportedSims: Array<{
      subscriptionId: number;
      slotIndex: number;
      carrierName: string;
      phoneNumber?: string;
    }>
  ): Promise<SimCard[]> {
    const existingSims = await this.listDeviceSims(uid, deviceId);
    const now = new Date().toISOString();
    const updatedSims: SimCard[] = [];

    const batch = collections.users().firestore.batch();

    for (const sim of reportedSims) {
      const existing = existingSims.find((s) => s.slotIndex === sim.slotIndex);
      const simId = existing ? existing.id : `sim_${deviceId}_slot${sim.slotIndex}`;

      let isVerified = false;
      let isAuthorizedFallback = false;

      // If existing SIM is identical in carrier and subscription ID, preserve previous authorization
      if (existing) {
        if (existing.subscriptionId === sim.subscriptionId && existing.carrierName === sim.carrierName) {
          isVerified = existing.isVerified;
          isAuthorizedFallback = existing.isAuthorizedFallback;
        } else {
          // Carrier or hardware swapped: revoke verification immediately!
          console.warn(`[SIM Change Detected] Device ${deviceId} SIM slot ${sim.slotIndex} changed from ${existing.carrierName} to ${sim.carrierName}. Revoking verification.`);
        }
      }

      const simCard: SimCard = {
        id: simId,
        subscriptionId: sim.subscriptionId,
        slotIndex: sim.slotIndex,
        carrierName: sim.carrierName,
        phoneNumber: sim.phoneNumber || existing?.phoneNumber,
        isVerified,
        isAuthorizedFallback,
        assignedBotId: existing?.assignedBotId,
        lastVerifiedAt: isVerified ? existing?.lastVerifiedAt : undefined,
        createdAt: existing?.createdAt || now,
        updatedAt: now,
      };

      const ref = collections.simDoc(uid, deviceId, simId);
      batch.set(ref, simCard);
      updatedSims.push(simCard);
    }

    await batch.commit();

    // Update sims array on device document
    await collections.deviceDoc(uid, deviceId).update({
      sims: updatedSims,
      updatedAt: now,
    });

    return updatedSims;
  }
}
