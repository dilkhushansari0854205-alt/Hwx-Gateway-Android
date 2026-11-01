import {
  generateApiKey,
  generateDeviceToken,
  hashString,
  generateHmacSignature,
  verifyHmacSignature,
  validateWebhookUrl,
  isPrivateOrReservedIp,
  redactSecrets,
} from '../../src/services/security.service.ts';

describe('Security Service Tests', () => {
  describe('API Key and Device Token Generation', () => {
    it('should generate an API key with correct prefix and hash', () => {
      const { rawKey, keyPrefix, keyHash } = generateApiKey();

      expect(rawKey.startsWith('hwx_live_')).toBe(true);
      expect(rawKey.length).toBeGreaterThan(40);
      expect(keyPrefix.startsWith('hwx_live_')).toBe(true);
      expect(keyHash).toBe(hashString(rawKey));
    });

    it('should generate unique API keys', () => {
      const key1 = generateApiKey();
      const key2 = generateApiKey();

      expect(key1.rawKey).not.toBe(key2.rawKey);
      expect(key1.keyHash).not.toBe(key2.keyHash);
    });

    it('should generate a device token with correct prefix', () => {
      const { rawToken, tokenHash } = generateDeviceToken();

      expect(rawToken.startsWith('hwx_dev_')).toBe(true);
      expect(tokenHash).toBe(hashString(rawToken));
    });
  });

  describe('HMAC-SHA256 Signatures', () => {
    const payload = JSON.stringify({ event: 'sms.received', message: 'Hello' });
    const secret = 'test_webhook_secret_key_123';

    it('should generate and verify matching HMAC signatures', () => {
      const signature = generateHmacSignature(payload, secret);
      expect(signature).toBeDefined();
      expect(typeof signature).toBe('string');

      const isValid = verifyHmacSignature(payload, secret, signature);
      expect(isValid).toBe(true);
    });

    it('should reject invalid or tampered HMAC signatures', () => {
      const signature = generateHmacSignature(payload, secret);
      const tamperedPayload = JSON.stringify({ event: 'sms.received', message: 'Hacked' });

      expect(verifyHmacSignature(tamperedPayload, secret, signature)).toBe(false);
      expect(verifyHmacSignature(payload, 'wrong_secret', signature)).toBe(false);
    });
  });

  describe('SSRF Protection & IP Range Validation', () => {
    it('should identify private, loopback, and carrier-grade NAT IPv4/IPv6 addresses', () => {
      expect(isPrivateOrReservedIp('127.0.0.1')).toBe(true);
      expect(isPrivateOrReservedIp('10.0.0.5')).toBe(true);
      expect(isPrivateOrReservedIp('100.64.0.1')).toBe(true); // CGNAT
      expect(isPrivateOrReservedIp('192.168.1.1')).toBe(true);
      expect(isPrivateOrReservedIp('172.16.0.1')).toBe(true);
      expect(isPrivateOrReservedIp('169.254.169.254')).toBe(true); // Cloud Metadata
      expect(isPrivateOrReservedIp('0.0.0.0')).toBe(true);
      expect(isPrivateOrReservedIp('224.0.0.1')).toBe(true); // Multicast
      expect(isPrivateOrReservedIp('255.255.255.255')).toBe(true); // Broadcast
      expect(isPrivateOrReservedIp('::1')).toBe(true);

      // Public IPs
      expect(isPrivateOrReservedIp('8.8.8.8')).toBe(false);
      expect(isPrivateOrReservedIp('1.1.1.1')).toBe(false);
    });

    it('should block local and private URLs from webhook delivery', async () => {
      const localhostCheck = await validateWebhookUrl('http://localhost:3000/webhook');
      expect(localhostCheck.valid).toBe(false);

      const metadataCheck = await validateWebhookUrl('http://169.254.169.254/latest/meta-data');
      expect(metadataCheck.valid).toBe(false);

      const gcpMetadataCheck = await validateWebhookUrl('http://metadata.google.internal/computeMetadata');
      expect(gcpMetadataCheck.valid).toBe(false);

      const ftpCheck = await validateWebhookUrl('ftp://example.com/file');
      expect(ftpCheck.valid).toBe(false);

      const credentialCheck = await validateWebhookUrl('http://user:password@example.com/hook');
      expect(credentialCheck.valid).toBe(false);
    });
  });

  describe('Secret Redaction in Logs', () => {
    it('should redact sensitive properties from log objects', () => {
      const sensitive = {
        [REDACTED-SECRET],
        private_key: '-----BEGIN PRIVATE KEY-----',
        deviceToken: 'hwx_dev_token',
        nested: {
          password: 'supersecretpass',
          username: 'admin',
        },
        publicInfo: 'order_1234',
      };

      const redacted = redactSecrets(sensitive);
      expect(redacted.apiKey).toBe('[REDACTED]');
      expect(redacted.private_key).toBe('[REDACTED]');
      expect(redacted.deviceToken).toBe('[REDACTED]');
      expect(redacted.nested.password).toBe('[REDACTED]');
      expect(redacted.nested.username).toBe('admin');
      expect(redacted.publicInfo).toBe('order_1234');
    });
  });
});
