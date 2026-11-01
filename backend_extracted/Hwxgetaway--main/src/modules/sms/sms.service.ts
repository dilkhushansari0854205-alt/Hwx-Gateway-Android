import crypto from 'crypto';
import { Query, QueryDocumentSnapshot } from 'firebase-admin/firestore';
import { collections } from '../../config/firestore.ts';
import { Bot, SimCard, SmsJob, SmsMessage } from '../../types/index.ts';
import { BotService } from '../bots/bot.service.ts';
import { DeviceService } from '../devices/device.service.ts';
import { DeviceSocketManager } from '../websocket/deviceSocket.ts';

export interface SendSmsInput {
  bot_id: string;
  device_id: string;
  to: string;
  message: string;
  sim_id?: string;
  reply_to_message_id?: string;
  idempotency_key?: string;
}

/**
 * HWX SMS Gateway - SMS Sending & Job Coordination Service
 *
 * Implements strict ownership verification, Bot-to-Device mapping enforcement,
 * Dual-SIM resolution, Reply SIM locking, idempotency protection, and live WebSocket dispatch.
 */
export class SmsService {
  /**
   * Resolves the target SIM card based on Bot mode, SIM authorization rules, and verification status.
   */
  public static resolveSim(
    bot: Bot,
    deviceSims: SimCard[],
    requestedSimId?: string
  ): SimCard {
    if (deviceSims.length === 0) {
      throw new Error('DEVICE_NO_SIM: Target device has no registered SIM cards');
    }

    // 1. Manual mode: must specify sim_id explicitly
    if (bot.simSelectionMode === 'manual') {
      if (!requestedSimId) {
        throw new Error('SIM_REQUIRED: Bot is in Manual SIM mode. Explicit sim_id must be provided.');
      }
      const sim = deviceSims.find((s) => s.id === requestedSimId);
      if (!sim) {
        throw new Error(`SIM_NOT_FOUND: Specified SIM '${requestedSimId}' does not exist on device`);
      }
      if (!sim.isVerified) {
        throw new Error(`SIM_NOT_VERIFIED: SIM '${requestedSimId}' is not verified`);
      }
      return sim;
    }

    // 2. Bot Fixed SIM mode: must use assigned SIM
    if (bot.simSelectionMode === 'bot_fixed') {
      const sim = deviceSims.find((s) => s.id === bot.assignedSimId);
      if (!sim) {
        throw new Error(`SIM_NOT_FOUND: Assigned Bot SIM '${bot.assignedSimId}' not found on device`);
      }
      if (!sim.isVerified) {
        throw new Error(`SIM_NOT_VERIFIED: Assigned Bot SIM '${bot.assignedSimId}' requires verification`);
      }
      return sim;
    }

    // 3. Auto Switch mode: uses primary SIM, or explicitly authorized fallback SIMs
    if (bot.simSelectionMode === 'auto_switch') {
      const primarySim = deviceSims.find((s) => s.id === bot.assignedSimId);
      if (primarySim && primarySim.isVerified) {
        return primarySim;
      }

      // Check authorized fallback SIMs only (must be both isVerified AND isAuthorizedFallback)
      const fallbackIds = bot.fallbackSimIds || [];
      for (const fallbackId of fallbackIds) {
        const fallbackSim = deviceSims.find((s) => s.id === fallbackId);
        if (fallbackSim && fallbackSim.isVerified && fallbackSim.isAuthorizedFallback) {
          console.log(`[Auto Switch] Primary SIM unavailable. Switching to authorized fallback SIM: ${fallbackSim.id}`);
          return fallbackSim;
        }
      }

      throw new Error('SIM_AUTO_SWITCH_EXHAUSTED: Primary SIM and all authorized fallback SIMs are unavailable or unverified');
    }

    throw new Error('SIM_AMBIGUOUS: Unable to resolve SIM selection. Check bot SIM configuration.');
  }

  /**
   * Orchestrates the complete SMS sending workflow with security gates.
   */
  static async sendSms(uid: string, input: SendSmsInput): Promise<SmsJob> {
    const now = new Date().toISOString();

    // 1. Idempotency Check: Prevent duplicate SMS dispatch
    if (input.idempotency_key) {
      const existingJobs = await collections
        .smsJobs(uid)
        .where('idempotencyKey', '==', input.idempotency_key)
        .limit(1)
        .get();

      if (!existingJobs.empty) {
        console.log(`[Idempotency] Returning existing SMS job for idempotency key: ${input.idempotency_key}`);
        return existingJobs.docs[0].data() as SmsJob;
      }
    }

    // 2. Verify Bot ownership and active status
    const bot = await BotService.getBot(uid, input.bot_id);
    if (!bot) {
      throw new Error(`BOT_NOT_FOUND: Bot '${input.bot_id}' does not exist or does not belong to user`);
    }
    if (bot.status !== 'active') {
      throw new Error(`BOT_INACTIVE: Bot '${input.bot_id}' is disabled`);
    }

    // 3. Verify Device registration and ownership
    const device = await DeviceService.getDevice(uid, input.device_id);
    if (!device) {
      throw new Error(`DEVICE_NOT_FOUND: Device '${input.device_id}' does not exist or does not belong to user`);
    }
    if (device.status === 'disabled') {
      throw new Error(`DEVICE_DISABLED: Device '${input.device_id}' is disabled`);
    }

    // 4. Verify Bot-to-Device mapping: SMS only through authorized device
    if (bot.assignedDeviceId !== device.id) {
      throw new Error(
        `BOT_DEVICE_MISMATCH: Bot '${bot.id}' is configured for device '${bot.assignedDeviceId}', but request specified device '${device.id}'`
      );
    }

    // 5. Dual-SIM Resolution & Reply SIM Locking
    const sims = await DeviceService.listDeviceSims(uid, input.device_id);
    let selectedSim: SimCard;

    if (input.reply_to_message_id) {
      // Rule 5: SMS reply must strictly use the original receiving SIM
      const originalMsgDoc = await collections.smsMessageDoc(uid, input.reply_to_message_id).get();
      if (!originalMsgDoc.exists) {
        throw new Error(`REPLY_MESSAGE_NOT_FOUND: Original message '${input.reply_to_message_id}' not found`);
      }

      const originalMsg = originalMsgDoc.data() as SmsMessage;
      if (originalMsg.direction !== 'inbound') {
        throw new Error('INVALID_REPLY_TARGET: Can only reply to an inbound SMS message');
      }

      // Check recipient matches original sender
      const normalizedTo = input.to.replace(/\s+/g, '');
      const normalizedFrom = originalMsg.from.replace(/\s+/g, '');
      if (normalizedTo !== normalizedFrom && !normalizedTo.endsWith(normalizedFrom) && !normalizedFrom.endsWith(normalizedTo)) {
        throw new Error(`REPLY_RECIPIENT_MISMATCH: Reply recipient '${input.to}' does not match original sender '${originalMsg.from}'`);
      }

      // Prohibit silent SIM switching for replies
      if (input.sim_id && originalMsg.simId && input.sim_id !== originalMsg.simId) {
        throw new Error(
          `SIM_REPLY_MISMATCH: Reply must use the original receiving SIM ('${originalMsg.simId}'). Silent SIM switching is forbidden.`
        );
      }

      // Locate the receiving SIM on this device
      const originalSim = sims.find((s) => s.id === originalMsg.simId || s.slotIndex === originalMsg.simSlot);
      if (!originalSim) {
        throw new Error(`SIM_RECEIVING_UNAVAILABLE: The SIM that received the message is no longer present on device '${device.id}'`);
      }
      if (!originalSim.isVerified) {
        throw new Error(`SIM_NOT_VERIFIED: The receiving SIM '${originalSim.id}' requires verification before sending replies`);
      }

      selectedSim = originalSim;
      console.log(`[SMS Reply] Locked sender SIM to original receiving SIM: ${selectedSim.id} (Slot ${selectedSim.slotIndex})`);
    } else {
      selectedSim = this.resolveSim(bot, sims, input.sim_id);
    }

    // 6. Create persistent SMS Job in Firestore
    const jobId = `job_${Date.now()}_${crypto.randomBytes(6).toString('hex')}`;

    const newJob: SmsJob = {
      id: jobId,
      uid,
      botId: bot.id,
      deviceId: device.id,
      simId: selectedSim.id,
      simSlot: selectedSim.slotIndex,
      to: input.to,
      from: selectedSim.phoneNumber || selectedSim.carrierName,
      message: input.message,
      status: 'queued',
      idempotencyKey: input.idempotency_key,
      replyToMessageId: input.reply_to_message_id,
      createdAt: now,
      updatedAt: now,
    };

    await collections.smsJobDoc(uid, jobId).set(newJob);

    // Create corresponding message log
    const initialMessage: SmsMessage = {
      id: jobId,
      uid,
      botId: bot.id,
      deviceId: device.id,
      simId: selectedSim.id,
      simSlot: selectedSim.slotIndex,
      direction: 'outbound',
      replyToMessageId: input.reply_to_message_id,
      from: selectedSim.phoneNumber || selectedSim.carrierName,
      to: input.to,
      message: input.message,
      createdAt: now,
    };
    await collections.smsMessageDoc(uid, jobId).set(initialMessage);

    // 7. Dispatch via WebSocket if device is currently connected
    const socketManager = DeviceSocketManager.getInstance();
    const isConnected = socketManager.isDeviceConnected(device.id);

    if (isConnected) {
      const dispatched = socketManager.sendSmsCommand(device.id, {
        type: 'COMMAND_SEND_SMS',
        commandId: `cmd_${jobId}`,
        jobId,
        to: input.to,
        message: input.message,
        simSlot: selectedSim.slotIndex,
        subscriptionId: selectedSim.subscriptionId,
      });

      if (dispatched) {
        newJob.status = 'sending';
        newJob.dispatchedAt = new Date().toISOString();
        newJob.updatedAt = newJob.dispatchedAt;
        await collections.smsJobDoc(uid, jobId).update({
          status: 'sending',
          dispatchedAt: newJob.dispatchedAt,
          updatedAt: newJob.updatedAt,
        });
      }
    } else {
      console.log(`[SMS Service] Device ${device.id} is offline. Job ${jobId} safely queued in Firestore.`);
    }

    return newJob;
  }

  /**
   * Retrieves paginated SMS logs.
   */
  static async listSmsLogs(
    uid: string,
    options: {
      botId?: string;
      deviceId?: string;
      direction?: 'inbound' | 'outbound';
      limit?: number;
    } = {}
  ): Promise<SmsMessage[]> {
    let query: Query = collections.smsMessages(uid);

    if (options.botId) {
      query = query.where('botId', '==', options.botId);
    }
    if (options.deviceId) {
      query = query.where('deviceId', '==', options.deviceId);
    }
    if (options.direction) {
      query = query.where('direction', '==', options.direction);
    }

    query = query.orderBy('createdAt', 'desc').limit(options.limit || 50);

    const snapshot = await query.get();
    return snapshot.docs.map((doc: QueryDocumentSnapshot) => doc.data() as SmsMessage);
  }

  /**
   * Retrieves a specific SMS record or job.
   */
  static async getSmsById(uid: string, messageId: string): Promise<SmsMessage | SmsJob | null> {
    const msgDoc = await collections.smsMessageDoc(uid, messageId).get();
    if (msgDoc.exists) {
      return msgDoc.data() as SmsMessage;
    }

    const jobDoc = await collections.smsJobDoc(uid, messageId).get();
    if (jobDoc.exists) {
      return jobDoc.data() as SmsJob;
    }

    return null;
  }
}
