import crypto from 'crypto';
import { QueryDocumentSnapshot } from 'firebase-admin/firestore';
import { collections } from '../../config/firestore.ts';
import { Bot, SimSelectionMode } from '../../types/index.ts';
import { DeviceService } from '../devices/device.service.ts';
import { validateWebhookUrl } from '../../services/security.service.ts';

/**
 * HWX SMS Gateway - Bot Management Service
 *
 * Implements isolated multi-bot management per user,
 * device & SIM assignments, Auto Switch fallback configuration, and webhooks.
 */
export class BotService {
  /**
   * Creates an independent Bot for a user.
   */
  static async createBot(
    uid: string,
    data: {
      name: string;
      description?: string;
      assignedDeviceId: string;
      assignedSimId: string;
      simSelectionMode?: SimSelectionMode;
      fallbackSimIds?: string[];
      webhookUrl?: string;
    }
  ): Promise<Bot> {
    const botId = `bot_${crypto.randomBytes(8).toString('hex')}`;
    const now = new Date().toISOString();

    // Verify assigned device belongs to user
    const device = await DeviceService.getDevice(uid, data.assignedDeviceId);
    if (!device) {
      throw new Error(`Device '${data.assignedDeviceId}' not found or does not belong to user`);
    }

    // Verify assigned SIM exists on device
    const sims = await DeviceService.listDeviceSims(uid, data.assignedDeviceId);
    const primarySim = sims.find((s) => s.id === data.assignedSimId);
    if (!primarySim) {
      throw new Error(`SIM '${data.assignedSimId}' not found on device '${data.assignedDeviceId}'`);
    }

    // Validate webhook URL if provided
    if (data.webhookUrl) {
      const urlCheck = await validateWebhookUrl(data.webhookUrl);
      if (!urlCheck.valid) {
        throw new Error(`Invalid webhook URL: ${urlCheck.reason}`);
      }
    }

    const simMode: SimSelectionMode = data.simSelectionMode || 'bot_fixed';

    // Verify fallback SIMs if auto_switch is enabled
    if (simMode === 'auto_switch' && data.fallbackSimIds && data.fallbackSimIds.length > 0) {
      for (const fallbackId of data.fallbackSimIds) {
        const found = sims.find((s) => s.id === fallbackId);
        if (!found) {
          throw new Error(`Fallback SIM '${fallbackId}' not found on assigned device`);
        }
        if (!found.isAuthorizedFallback) {
          throw new Error(`SIM '${fallbackId}' is not authorized as an Auto Switch fallback`);
        }
      }
    }

    const bot: Bot = {
      id: botId,
      uid,
      name: data.name,
      description: data.description || '',
      assignedDeviceId: data.assignedDeviceId,
      assignedSimId: data.assignedSimId,
      simSelectionMode: simMode,
      fallbackSimIds: data.fallbackSimIds || [],
      webhookUrl: data.webhookUrl || '',
      status: 'active',
      createdAt: now,
      updatedAt: now,
    };

    await collections.botDoc(uid, botId).set(bot);

    // Update SIM record with assigned bot
    await collections.simDoc(uid, data.assignedDeviceId, data.assignedSimId).update({
      assignedBotId: botId,
      updatedAt: now,
    });

    return bot;
  }

  /**
   * Lists all bots owned by a user.
   */
  static async listBots(uid: string): Promise<Bot[]> {
    const snapshot = await collections.bots(uid).orderBy('createdAt', 'desc').get();
    return snapshot.docs.map((doc: QueryDocumentSnapshot) => doc.data() as Bot);
  }

  /**
   * Retrieves a specific bot.
   */
  static async getBot(uid: string, botId: string): Promise<Bot | null> {
    const doc = await collections.botDoc(uid, botId).get();
    if (!doc.exists) return null;
    return doc.data() as Bot;
  }

  /**
   * Updates an existing Bot's configuration.
   */
  static async updateBot(
    uid: string,
    botId: string,
    updates: Partial<{
      name: string;
      description: string;
      assignedDeviceId: string;
      assignedSimId: string;
      simSelectionMode: SimSelectionMode;
      fallbackSimIds: string[];
      webhookUrl: string;
      status: 'active' | 'inactive';
    }>
  ): Promise<Bot> {
    const existing = await this.getBot(uid, botId);
    if (!existing) {
      throw new Error(`Bot '${botId}' not found`);
    }

    const targetDeviceId = updates.assignedDeviceId || existing.assignedDeviceId;
    const targetSimId = updates.assignedSimId || existing.assignedSimId;

    if (updates.assignedDeviceId || updates.assignedSimId) {
      const device = await DeviceService.getDevice(uid, targetDeviceId);
      if (!device) {
        throw new Error(`Device '${targetDeviceId}' does not belong to user`);
      }
      const sims = await DeviceService.listDeviceSims(uid, targetDeviceId);
      const targetSim = sims.find((s) => s.id === targetSimId);
      if (!targetSim) {
        throw new Error(`SIM '${targetSimId}' does not exist on device '${targetDeviceId}'`);
      }
    }

    if (updates.webhookUrl) {
      const urlCheck = await validateWebhookUrl(updates.webhookUrl);
      if (!urlCheck.valid) {
        throw new Error(`Invalid webhook URL: ${urlCheck.reason}`);
      }
    }

    const now = new Date().toISOString();
    const updatedData: Partial<Bot> = {
      ...updates,
      updatedAt: now,
    };

    await collections.botDoc(uid, botId).update(updatedData);
    return { ...existing, ...updatedData };
  }

  /**
   * Deletes a Bot.
   */
  static async deleteBot(uid: string, botId: string): Promise<boolean> {
    const existing = await this.getBot(uid, botId);
    if (!existing) return false;

    await collections.botDoc(uid, botId).delete();

    // Clear assignedBotId on the SIM
    await collections
      .simDoc(uid, existing.assignedDeviceId, existing.assignedSimId)
      .update({
        assignedBotId: null,
        updatedAt: new Date().toISOString(),
      })
      .catch(() => {});

    return true;
  }

  /**
   * Finds the bot assigned to a given device and SIM slot.
   */
  static async findBotByDeviceAndSim(
    uid: string,
    deviceId: string,
    simIdOrSlot: string | number
  ): Promise<Bot | null> {
    const bots = await this.listBots(uid);
    const activeBots = bots.filter((b) => b.status === 'active' && b.assignedDeviceId === deviceId);

    if (activeBots.length === 0) return null;

    // Check primary assignment
    const directMatch = activeBots.find((b) => b.assignedSimId === simIdOrSlot);
    if (directMatch) return directMatch;

    // Check if slot index matches
    const sims = await DeviceService.listDeviceSims(uid, deviceId);
    const matchingSim = sims.find((s) => s.id === simIdOrSlot || s.slotIndex === simIdOrSlot);
    if (matchingSim) {
      const botForSim = activeBots.find((b) => b.assignedSimId === matchingSim.id);
      if (botForSim) return botForSim;
    }

    return null;
  }
}
