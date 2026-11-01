import { SmsService } from '../../src/modules/sms/sms.service.ts';
import { Bot, SimCard } from '../../src/types/index.ts';

describe('Dual SIM Management & Selection Logic', () => {
  const baseDeviceSims: SimCard[] = [
    {
      id: 'sim_slot_0',
      subscriptionId: 1,
      slotIndex: 0,
      carrierName: 'Carrier A',
      isVerified: true,
      isAuthorizedFallback: true,
      createdAt: '2026-01-01T00:00:00Z',
      updatedAt: '2026-01-01T00:00:00Z',
    },
    {
      id: 'sim_slot_1',
      subscriptionId: 2,
      slotIndex: 1,
      carrierName: 'Carrier B',
      isVerified: true,
      isAuthorizedFallback: true,
      createdAt: '2026-01-01T00:00:00Z',
      updatedAt: '2026-01-01T00:00:00Z',
    },
  ];

  describe('Bot Fixed SIM Mode', () => {
    const fixedBot: Bot = {
      id: 'bot_fixed_1',
      uid: 'user_1',
      name: 'Fixed SIM Bot',
      assignedDeviceId: 'dev_1',
      assignedSimId: 'sim_slot_0',
      simSelectionMode: 'bot_fixed',
      status: 'active',
      createdAt: '2026-01-01T00:00:00Z',
      updatedAt: '2026-01-01T00:00:00Z',
    };

    it('should select the assigned SIM when verified', () => {
      const selected = SmsService.resolveSim(fixedBot, baseDeviceSims);
      expect(selected.id).toBe('sim_slot_0');
      expect(selected.slotIndex).toBe(0);
    });

    it('should throw SIM_NOT_VERIFIED if assigned SIM requires verification', () => {
      const unverifiedSims: SimCard[] = [
        { ...baseDeviceSims[0], isVerified: false },
        baseDeviceSims[1],
      ];

      expect(() => SmsService.resolveSim(fixedBot, unverifiedSims)).toThrow('SIM_NOT_VERIFIED');
    });

    it('should throw SIM_NOT_FOUND if assigned SIM is removed from device', () => {
      const missingSims: SimCard[] = [baseDeviceSims[1]];
      expect(() => SmsService.resolveSim(fixedBot, missingSims)).toThrow('SIM_NOT_FOUND');
    });
  });

  describe('Manual SIM Mode', () => {
    const manualBot: Bot = {
      id: 'bot_manual_1',
      uid: 'user_1',
      name: 'Manual Bot',
      assignedDeviceId: 'dev_1',
      assignedSimId: 'sim_slot_0',
      simSelectionMode: 'manual',
      status: 'active',
      createdAt: '2026-01-01T00:00:00Z',
      updatedAt: '2026-01-01T00:00:00Z',
    };

    it('should select the explicitly requested SIM when verified', () => {
      const selected = SmsService.resolveSim(manualBot, baseDeviceSims, 'sim_slot_1');
      expect(selected.id).toBe('sim_slot_1');
      expect(selected.slotIndex).toBe(1);
    });

    it('should throw SIM_REQUIRED when no sim_id is passed in manual mode', () => {
      expect(() => SmsService.resolveSim(manualBot, baseDeviceSims)).toThrow('SIM_REQUIRED');
    });

    it('should throw SIM_NOT_FOUND if requested SIM does not exist', () => {
      expect(() => SmsService.resolveSim(manualBot, baseDeviceSims, 'sim_unknown')).toThrow('SIM_NOT_FOUND');
    });

    it('should throw SIM_NOT_VERIFIED if requested manual SIM is unverified', () => {
      const unverifiedSims: SimCard[] = [
        baseDeviceSims[0],
        { ...baseDeviceSims[1], isVerified: false },
      ];
      expect(() => SmsService.resolveSim(manualBot, unverifiedSims, 'sim_slot_1')).toThrow('SIM_NOT_VERIFIED');
    });
  });

  describe('Auto Switch Mode', () => {
    const autoSwitchBot: Bot = {
      id: 'bot_auto_1',
      uid: 'user_1',
      name: 'Auto Switch Bot',
      assignedDeviceId: 'dev_1',
      assignedSimId: 'sim_slot_0',
      simSelectionMode: 'auto_switch',
      fallbackSimIds: ['sim_slot_1'],
      status: 'active',
      createdAt: '2026-01-01T00:00:00Z',
      updatedAt: '2026-01-01T00:00:00Z',
    };

    it('should use primary SIM when verified', () => {
      const selected = SmsService.resolveSim(autoSwitchBot, baseDeviceSims);
      expect(selected.id).toBe('sim_slot_0');
    });

    it('should fall back to authorized fallback SIM if primary is unverified', () => {
      const simsWithUnverifiedPrimary: SimCard[] = [
        { ...baseDeviceSims[0], isVerified: false },
        baseDeviceSims[1],
      ];

      const selected = SmsService.resolveSim(autoSwitchBot, simsWithUnverifiedPrimary);
      expect(selected.id).toBe('sim_slot_1');
    });

    it('should reject fallback if fallback SIM is not authorized', () => {
      const simsWithUnauthorizedFallback: SimCard[] = [
        { ...baseDeviceSims[0], isVerified: false },
        { ...baseDeviceSims[1], isAuthorizedFallback: false },
      ];

      expect(() => SmsService.resolveSim(autoSwitchBot, simsWithUnauthorizedFallback)).toThrow(
        'SIM_AUTO_SWITCH_EXHAUSTED'
      );
    });

    it('should reject fallback if fallback SIM is unverified even if authorized', () => {
      const simsWithUnverifiedFallback: SimCard[] = [
        { ...baseDeviceSims[0], isVerified: false },
        { ...baseDeviceSims[1], isVerified: false, isAuthorizedFallback: true },
      ];

      expect(() => SmsService.resolveSim(autoSwitchBot, simsWithUnverifiedFallback)).toThrow(
        'SIM_AUTO_SWITCH_EXHAUSTED'
      );
    });
  });
});
